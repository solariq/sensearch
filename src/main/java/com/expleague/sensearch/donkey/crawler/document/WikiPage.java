package com.expleague.sensearch.donkey.crawler.document;

public class WikiPage implements CrawlerDocument {

  private long id;
  private String title;
  private CharSequence page;
    /*
    String revision;
    String type;
    String nsId;
    //*/

  public void setId(long id) {
    this.id = id;
  }

  public void setPage(CharSequence page) {
    this.page = page;
  }

  @Override
  public String getTitle() {
    return this.title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public CharSequence getContent() {
    return this.page;
  }

  @Override
  public Long getID() {
    return this.id;
  }

  @Override
  public String toString() {
    return this.page.toString();
  }
}