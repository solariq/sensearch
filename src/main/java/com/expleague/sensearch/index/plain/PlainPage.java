package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import java.net.URI;
import java.util.List;

public class PlainPage implements IndexedPage {

  private final long id;
  private final String text;
  private final String title;
  private final URI uri;
  private List<CharSequence> categories;
  private List<Section> sections;

  PlainPage(IndexUnits.Page page) {
    text = page.getContent();
    title = page.getTitle();
    uri = URI.create(page.getUri());
    id = page.getPageId();
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
  public CharSequence text() {
    return text;
  }

  @Override
  public List<CharSequence> categories() {
    return categories;
  }

  @Override
  public List<Section> sections() {
    return sections;
  }

  @Override
  public List<Page> inputLinks() {
    return null;
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

  public static class PageSection implements Section {

    private CharSequence text;
    private List<CharSequence> title;
    private List<Link> links;

    public PageSection(
        CharSequence text,
        List<CharSequence> title,
        List<Link> links
    ) {
      this.text = text;
      this.title = title;
      this.links = links;
    }

    @Override
    public CharSequence text() {
      return text;
    }

    @Override
    public List<CharSequence> title() {
      return title;
    }

    @Override
    public List<Link> links() {
      return links;
    }
  }

  public static class PageLink implements Link {

    private CharSequence text;
    private CharSequence context;
    private int linkOffset;

    public PageLink(
        CharSequence text,
        CharSequence context,
        int offset
    ) {
      this.text = text;
      this.context = context;
      this.linkOffset = offset;
    }

    @Override
    public CharSequence text() {
      return text;
    }

    @Override
    public CharSequence context() {
      return context;
    }

    //TODO: need to talk about this method
    @Override
    public Page targetPage() {
      return null;
    }

    @Override
    public int textOffset() {
      return linkOffset;
    }
  }
}
