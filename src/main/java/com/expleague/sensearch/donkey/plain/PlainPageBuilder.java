package com.expleague.sensearch.donkey.plain;


import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import java.nio.file.Path;

class PlainPageBuilder {

  private static final long PAGE_ID_SHIFT = 1L << 32;

  private final Path plainRoot;

  private long flushedPagesCount;

  PlainPageBuilder(Path plainRoot) {
    this.plainRoot = plainRoot;
    this.flushedPagesCount = 0;
  }

  void createAndFlushNewPage(CrawlerDocument crawlerDocument) {
    ++flushedPagesCount;
    // TODO: save page to disk
  }

  long currentDocumentId() {
    return flushedPagesCount + PAGE_ID_SHIFT;
  }
}
