package com.expleague.sensearch.donkey.crawler.document;

public interface CrawlerDocument {

  CharSequence getContent();

  String getTitle();

  Long getID();
}
