package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.google.common.primitives.Longs;
import java.io.IOException;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

class PlainPageBuilder {

  private static final int MAX_PAGES_IN_BATCH = 100;

  private static final WriteOptions DEFAULT_WRITE_OPTIONS =
      new WriteOptions().sync(true).snapshot(false);

  private final DB plainDb;

  private int negFlushedPagesCount = 0;

  private WriteBatch writeBatch = null;
  private int pagesInBatch = 0;

  PlainPageBuilder(DB plainDb) {
    this.plainDb = plainDb;
  }

  long add(CrawlerDocument newPage) {
    if (writeBatch == null) {
      writeBatch = plainDb.createWriteBatch();
    }

    if (pagesInBatch >= MAX_PAGES_IN_BATCH) {
      plainDb.write(writeBatch, DEFAULT_WRITE_OPTIONS);
      pagesInBatch = 0;
      writeBatch = plainDb.createWriteBatch();
    }

    byte[] pageIdBytes = Longs.toByteArray(negFlushedPagesCount);
    byte[] pageBytes =
        IndexUnits.Page.newBuilder()
            .setPageId(negFlushedPagesCount)
            .setContent(newPage.content().toString())
            .setTitle(newPage.title())
            .setUri(newPage.uri().toString())
            .build()
            .toByteArray();

    writeBatch.put(pageIdBytes, pageBytes);
    ++pagesInBatch;

    return negFlushedPagesCount--;
  }

  void build() throws IOException {
    if (pagesInBatch > 0) {
      plainDb.write(writeBatch, DEFAULT_WRITE_OPTIONS);
    }

    plainDb.close();
  }
}
