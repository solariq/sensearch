package com.expleague.sensearch.donkey.crawler.document;

import java.util.ArrayList;
import java.util.List;

public class WikiPage implements CrawlerDocument {

  private long id;
  private String title;
  private CharSequence page;
  private List<String> categories = new ArrayList<>();

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

  public void setTitle(String title) {
    this.title = title;
  }

  public void setCategories(List<String> categories) {
    this.categories = categories;
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
  public CharSequence content() {
    return this.page;
  }

  @Override
  public Long iD() {
    return this.id;
  }

  @Override
  public String toString() {
    return this.page.toString();
  }
}
