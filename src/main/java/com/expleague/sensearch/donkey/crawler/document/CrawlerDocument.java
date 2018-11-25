package com.expleague.sensearch.donkey.crawler.document;

import java.util.List;

public interface CrawlerDocument {

  CharSequence content();

  String title();

  List<String> categories();

  List<Section> sections();

  long iD();

  interface Section {
    CharSequence text();
    CharSequence title();
  }

}
