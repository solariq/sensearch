package com.expleague.sensearch.donkey.plain;


import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class PlainPageBuilder {

  private final Path plainRoot;

  private int flushedPagesCount = 1;

  PlainPageBuilder(Path plainRoot) throws IOException {
    Files.createDirectories(plainRoot);
    this.plainRoot = plainRoot;
    this.flushedPagesCount = 0;
  }

  void createAndFlushNewPage(CrawlerDocument crawlerDocument) {
    ++flushedPagesCount;
    // TODO: save page to disk
  }

  int currentDocumentId() {
    return -flushedPagesCount - 1;
  }
}
