package com.expleague.sensearch.donkey.crawler.document;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

public class WikiPage implements CrawlerDocument {

  private long id;
  private String title;
  private List<String> categories;
  private List<Section> sections;

  public void setUri(URI uri) {
    this.uri = uri;
  }

  private URI uri;

  /*
  String revision;
  String type;
  String nsId;
  //*/

  public void setId(long id) {
    this.id = id;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setCategories(List<String> categories) {
    this.categories = categories;
  }

  public void setSections(List<Section> sections) {
    this.sections = sections;
  }

  @Override
  public String title() {
    return this.title;
  }

  @Override
  public List<String> categories() {
    return this.categories;
  }

  @Override
  public Stream<Section> sections() {
    return sections.stream();
  }

  @Override
  public CharSequence content() {
    StringBuilder page = new StringBuilder();
    for (Section section : sections) {
      page.append(section.text());
      if (page.length() > 0) {
        page.append("\n\n\n");
      }
    }
    return page;
  }

  @Override
  public long id() {
    return this.id;
  }

  @Override
  public URI uri() {
    return uri;
  }

  public static class WikiSection implements Section {

    private final CharSequence text;
    private final List<CharSequence> title;
    private final List<Link> links;
    private final URI uri;

    public WikiSection(CharSequence text, List<CharSequence> title, List<Link> links, URI uri) {
      this.text = text;
      this.title = title;
      this.links = links;
      this.uri = uri;
    }

    @Override
    public CharSequence text() {
      return text;
    }

    @Override
    public URI uri() {
      return uri;
    }

    @Override
    public List<CharSequence> titles() {
      return title;
    }

    @Override
    public CharSequence title() {
      return title.get(title.size() - 1);
    }

    @Override
    public List<Link> links() {
      return links;
    }
  }

  public static class WikiLink implements Link {

    private final CharSequence text;
    private final CharSequence targetTitle;
    private final long targetId;
    private final int textOffset;

    public WikiLink(CharSequence text, CharSequence targetTitle, long targetId, int textOffset) {
      this.text = text;
      this.targetTitle = targetTitle;
      this.targetId = targetId;
      this.textOffset = textOffset;
    }

    @Override
    public CharSequence text() {
      return text;
    }

    @Override
    public CharSequence targetTitle() {
      return targetTitle;
    }

    @Override
    public long targetId() {
      return targetId;
    }

    @Override
    public int textOffset() {
      return textOffset;
    }
  }
}
