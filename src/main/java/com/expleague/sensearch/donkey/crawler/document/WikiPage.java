package com.expleague.sensearch.donkey.crawler.document;

import java.net.URI;
import java.util.List;

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
  public List<Section> sections() {
    return sections;
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

    private CharSequence text;
    private List<CharSequence> title;
    private List<Link> links;

    public WikiSection(CharSequence text, List<CharSequence> title, List<Link> links) {
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

  public static class WikiLink implements Link {

    private CharSequence text;
    private CharSequence targetTitle;
    private long targetId;
    private int textOffset;

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
