package com.expleague.sensearch.donkey.plain;


import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class PlainPageBuilder {

  private static final long PAGE_ID_SHIFT = 1L << 32;
  private int flushedPagesCount = 0;

  PlainPageBuilder() throws IOException {
  }

  long add(CrawlerDocument newPage) {
    ++flushedPagesCount;
    return PAGE_ID_SHIFT + flushedPagesCount;
  }

  void build(Path plainPath) {

  }
}
