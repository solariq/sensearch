package com.expleague.sensearch.index.plain;

import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.ws.rs.NotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlainPage implements IndexedPage {

  public static final IndexedPage EMPTY_PAGE = new IndexedPage() {
    private final URI DEFAULT_URI = URI.create("https://www.wikipedia.org/");
    private static final String EMPTY_STRING = "";
    @Override
    public long id() {
      throw new NotSupportedException("Id is not determined for an EmptyPage");
    }

    @Override
    public long parentId() {
      throw new NotSupportedException("Parent id is not determined for an EmptyPage");
    }

    @Override
    public LongStream subpagesIds() {
      return LongStream.empty();
    }

    @Override
    public URI uri() {
      return DEFAULT_URI;
    }

    @Override
    public CharSequence title() {
      return EMPTY_STRING;
    }

    @Override
    public CharSequence fullContent() {
      return EMPTY_STRING;
    }

    @Override
    public CharSequence content() {
      return EMPTY_STRING;
    }

    @Override
    public List<CharSequence> categories() {
      return Collections.emptyList();
    }

    @Override
    public Stream<Link> outgoingLinks() {
      return Stream.empty();
    }

    @Override
    public Stream<Link> incomingLinks() {
      return Stream.empty();
    }

    @Override
    public Page parent() {
      return this;
    }

    @Override
    public Stream<Page> subpages() {
      return Stream.empty();
    }

    @Override
    public Stream<CharSequence> sentences() {
      return Stream.empty();
    }

    @Override
    public Stream<Term> parse(CharSequence sequence) {
      return Stream.empty();
    }
  };

  private static final Logger LOG = LoggerFactory.getLogger(PlainPage.class);

  private final long id;
  private final PlainIndex index;

  private IndexUnits.Page protoPage;

  private URI uri;

  private boolean isEmpty;

  private PlainPage(IndexUnits.Page protoPage, PlainIndex index) {
    this.index = index;
    this.protoPage = protoPage;
    this.uri = URI.create(protoPage.getUri());

    id = protoPage.getPageId();
    isEmpty = false;
  }

  static IndexedPage create(long id, PlainIndex plainIndex) {
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
    if (protoPage.hasParentId()) {
      return protoPage.getParentId();
    }
    return protoPage.getParentId();
  }

  @Override
  public LongStream subpagesIds() {
    if (protoPage.getSubpagesIdsCount() == 0) {
      return LongStream.empty();
    }

    return protoPage.getSubpagesIdsList().stream().mapToLong(Number::longValue);
  }

  @Override
  public URI uri() {
    return uri;
  }

  @Override
  public CharSequence content() {
    return protoPage.getContent();
  }

  @Override
  public CharSequence fullContent() {
    return CharSeqTools.concat(content(), "\n", subpages().map(Page::fullContent).collect(Collectors.joining("\n")));
  }

  @Override
  public List<CharSequence> categories() {
    return protoPage.getCategoriesList()
        .stream()
        .map(CharSequence.class::cast)
        .collect(Collectors.toList());
  }

  @Override
  public Stream<Link> outgoingLinks() {
    if (protoPage.getOutgoingLinksCount() == 0) {
      return Stream.empty();
    }

    return protoPage.getOutgoingLinksList()
        .stream()
        .map(l -> PlainLink.withSource(l, index, this));
  }

  @Override
  public Stream<Link> incomingLinks() {
    if (protoPage.getIncomingLinksCount() == 0) {
      return Stream.empty();
    }

    return protoPage.getIncomingLinksList()
        .stream()
        .map(l -> PlainLink.withTarget(l, index, this));
  }

  @Override
  public Page parent() {
    if (protoPage.hasParentId()) {
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
  public Stream<CharSequence> sentences() {
    return index.sentences(content());
  }

  @Override
  public Stream<Term> parse(CharSequence sequence) {
    return index.parse(sequence);
  }

  @Override
  public CharSequence title() {
    return protoPage.getTitle();
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
