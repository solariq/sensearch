package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlainPage implements IndexedPage {

  public static final PlainPage EMPTY_PAGE = new PlainPage();
  private static final String EMPTY_STRING = "";
  private static final URI DEFAULT_URI = URI.create("https://www.wikipedia.org/");

  private static final Logger LOG = LoggerFactory.getLogger(PlainPage.class);

  private final long id;
  private final PlainIndex index;

  private IndexUnits.Page protoPage;

  private URI uri;

  private boolean isEmpty;

  private PlainPage() {
    isEmpty = true;
    id = 0;
    index = null;
  }

  private PlainPage(IndexUnits.Page protoPage, PlainIndex index) {
    this.index = index;
    this.protoPage = protoPage;
    this.uri = URI.create(protoPage.getUri());

    id = protoPage.getPageId();
    isEmpty = false;
  }

  static PlainPage create(long id, PlainIndex plainIndex) {
    try {
      IndexUnits.Page protoPage = plainIndex.protoPageLoad(id);
      return new PlainPage(protoPage, plainIndex);
    } catch (NoSuchElementException e) {
      LOG.warn(String.format("No page was found in the index by given id [ %d ]."
          + " Returned empty page", id));
      return EMPTY_PAGE;
    } catch (InvalidProtocolBufferException e) {
      LOG.warn(String.format("Encountered invalid protobuf for the page with id [ %d ]."
          + " Empty page is returned. Cause: %s", id, e.toString())
      );
      return EMPTY_PAGE;
    }
  }

  @Override
  public long id() {
    if (isEmpty) {
      throw new UnsupportedOperationException("There is no page id for an empty page!");
    }
    return this.id;
  }

  @Override
  public long parentId() {
    if (isEmpty) {
      throw new UnsupportedOperationException("There is no parent id for an empty page!");
    }
    if (protoPage.hasParentId()) {
      return protoPage.getParentId();
    }
    return protoPage.getParentId();
  }

  @Override
  public LongStream subpagesIds() {
    if (isEmpty || protoPage.getSubpagesIdsCount() == 0) {
      return LongStream.empty();
    }

    return protoPage.getSubpagesIdsList().stream().mapToLong(Number::longValue);
  }

  @Override
  public URI uri() {
    if (isEmpty) {
      return DEFAULT_URI;
    }
    return uri;
  }

  @Override
  public CharSequence content() {
    if (isEmpty) {
      return EMPTY_STRING;
    }
    return protoPage.getContent();
  }

  @Override
  // TODO: implement
  public CharSequence fullContent() {
    if (isEmpty) {
      return EMPTY_STRING;
    }

    return EMPTY_STRING;
  }

  @Override
  public List<CharSequence> categories() {
    return Collections.emptyList();
  }

  @Override
  public Stream<Link> outcomingLinks() {
    if (isEmpty || protoPage.getOutcomingLinksCount() == 0) {
      return Stream.empty();
    }

    return protoPage.getOutcomingLinksList()
        .stream()
        .map(l -> PlainLink.withSource(l, index, this));
  }

  @Override
  public Stream<Link> incomingLinks() {
    if (isEmpty || protoPage.getIncomingLinksCount() == 0) {
      return Stream.empty();
    }

    return protoPage.getIncomingLinksList()
        .stream()
        .map(l -> PlainLink.withTarget(l, index, this));
  }

  @Override
  public Page parent() {
    if (!isEmpty && protoPage.hasParentId()) {
      PlainPage.create(protoPage.getParentId(), this.index);
    }

    return this;
  }

  @Override
  public Stream<Page> subpages() {
    return this
        .subpagesIds()
        .mapToObj(id -> PlainPage.create(id, this.index));
  }

  @Override
  public CharSequence title() {
    if (isEmpty) {
      return EMPTY_STRING;
    }

    return protoPage.getTitle();
  }

  @Override
  public boolean isEmpty() {
    return isEmpty;
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof PlainPage) && (this == other || ((PlainPage) other).id == this.id);
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.id);
  }

  static class PlainLink implements Page.Link {

    private static final Logger LOG = LoggerFactory.getLogger(PlainLink.class);

    private final PlainIndex index;
    private final IndexUnits.Page.Link protoLink;
    private final IndexedPage sourcePage;
    private final IndexedPage targetPage;

    PlainLink(IndexUnits.Page.Link protoLink, PlainIndex index, IndexedPage sourcePage,
        IndexedPage targetPage) {
      this.protoLink = protoLink;
      this.index = index;
      this.sourcePage = sourcePage;
      this.targetPage = targetPage;
    }

    static PlainLink withTarget(IndexUnits.Page.Link protoLink, PlainIndex index,
        PlainPage targetPage) {
      return new PlainLink(
          protoLink,
          index,
          PlainPage.create(protoLink.getSourcePageId(), index),
          targetPage
      );
    }

    static PlainLink withSource(IndexUnits.Page.Link protoLink, PlainIndex index,
        PlainPage sourcePage) {
      return new PlainLink(
          protoLink,
          index,
          sourcePage,
          protoLink.hasTargetPageId() ? PlainPage.create(protoLink.getTargetPageId(), index) :
              PlainPage.EMPTY_PAGE
      );
    }

    static PlainLink fromProtoLinkOnly(IndexUnits.Page.Link protoLink, PlainIndex index) {
      return new PlainLink(
          protoLink,
          index,
          PlainPage.create(protoLink.getSourcePageId(), index),
          protoLink.hasTargetPageId() ? PlainPage.create(protoLink.getTargetPageId(), index) :
              PlainPage.EMPTY_PAGE
      );
    }

    @Override
    public boolean hasTarget() {
      return protoLink.hasTargetPageId();
    }

    @Override
    public CharSequence text() {
      return protoLink.getText();
    }

    @Override
    public Page targetPage() {
      return targetPage;
    }

    @Override
    public Page sourcePage() {
      return sourcePage;
    }

    @Override
    public long position() {
      return protoLink.getPosition();
    }
  }

}
