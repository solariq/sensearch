package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PlainPage implements IndexedPage {

  private final long id;
  private final URI uri;

  private final String title;
  private final String content;

  private final List<Link> outcomingLinks;
  private final List<Link> incomingLinks;

  private final long parentID;
  private final List<Link> subpagesLinks;

  private List<CharSequence> categories = new ArrayList<>();

  PlainPage(IndexUnits.Page page) {
    id = page.getPageId();
    uri = URI.create(page.getUri());

    title = page.getTitle();
    content = page.getContent();

    List<Link> links = new ArrayList<>();
    Link tmp;

    for (int ind = 0; ind < page.getOutcomingLinksCount(); ind++) {
      tmp = new PageLink(page.getOutcomingLinks(ind).getText(),
          page.getOutcomingLinks(ind).getPosition(),
          page.getOutcomingLinks(ind).getTargetPageId(),
          page.getOutcomingLinks(ind).getSourcePageId()
          );
      links.add(tmp);
    }
    outcomingLinks = links;

    links.clear();
    for (int ind = 0; ind < page.getIncomingLinksCount(); ind++) {
      tmp = new PageLink(page.getIncomingLinks(ind).getText(),
          page.getIncomingLinks(ind).getPosition(),
          page.getIncomingLinks(ind).getTargetPageId(),
          page.getIncomingLinks(ind).getSourcePageId()
      );
      links.add(tmp);
    }
    incomingLinks = links;

    parentID = page.getParentId();

    links.clear();
    for (int ind = 0; ind < page.getSubpagesIdsCount(); ind++) {
      tmp = new PageLink("",
          0,
          page.getSubpagesIds(ind),
          id
      );
      links.add(tmp);
    }
    subpagesLinks = links;
  }

  @Override
  public long id() {
    return this.id;
  }

  @Override
  public URI reference() {
    return uri;
  }

  @Override
  public CharSequence content() {
    return content;
  }

  @Override
  public List<CharSequence> categories() {
    return categories;
  }

  @Override
  public Stream<Link> outcomingLinks() {
    return outcomingLinks.stream();
  }

  @Override
  public Stream<Link> incomingLinks() {
    return incomingLinks.stream();
  }

  @Override
  public long parent() {
    return parentID;
  }

  @Override
  public Stream<Link> subpages() {
    return subpagesLinks.stream();
  }

  @Override
  public CharSequence title() {
    return title;
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof PlainPage) && (this == other || ((PlainPage) other).id == this.id);
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.id);
  }


  public static class PageLink implements Link {

    private CharSequence text;
    private long linkOffset;

    private long tagetLink;
    private long sourceLink;

    PageLink(
        CharSequence text,
        long offset,
        long target,
        long source
    ) {
      this.text = text;
      this.linkOffset = offset;
      this.tagetLink = target;
      this.sourceLink = source;
    }

    @Override
    public CharSequence text() {
      return text;
    }

    @Override
    public long targetPage() {
      return tagetLink;
    }

    @Override
    public long sourcePage() {
      return sourceLink;
    }

    @Override
    public long textOffset() {
      return linkOffset;
    }
  }
}
