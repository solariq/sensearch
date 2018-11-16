package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import java.io.IOException;
import java.nio.file.Path;

class PlainPageBuilder {

  private int flushedPagesCount = 0;

  PlainPageBuilder() throws IOException {
  }

  int add(CrawlerDocument newPage) {
    ++flushedPagesCount;
    return flushedPagesCount;
  }

  void build(Path plainPath) {

  }
}
