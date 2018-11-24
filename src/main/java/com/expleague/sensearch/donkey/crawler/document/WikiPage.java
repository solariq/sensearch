package com.expleague.sensearch.donkey.crawler.document;

import java.util.List;

public class WikiPage implements CrawlerDocument {

  private long id;
  private String title;
  private List<String> categories;
  private List<Section> sections;

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
  public long iD() {
    return this.id;
  }

  public static class WikiSection implements Section {

    private CharSequence text;
    private CharSequence title;

    public WikiSection(CharSequence text, CharSequence title) {
      this.text = text;
      this.title = title;
    }

    @Override
    public CharSequence text() {
      return text;
    }

    @Override
    public CharSequence title() {
      return title;
    }
  }
}
