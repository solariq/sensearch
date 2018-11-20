package com.expleague.sensearch.donkey.crawler.document;

import java.util.List;

public interface CrawlerDocument {

  CharSequence content();

  String title();

  List<String> categories();

  Long iD();
}
