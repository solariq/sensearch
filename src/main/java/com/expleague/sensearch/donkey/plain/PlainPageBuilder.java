package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

class PlainPageBuilder {

  private static final long DEFAULT_CACHE_SIZE = 16 * (1 << 20); // 16 MB

  private static final int MAX_PAGES_IN_BATCH = 100;

  private static final Options DEFAULT_DB_OPTIONS = new Options()
      .cacheSize(DEFAULT_CACHE_SIZE)
      .createIfMissing(true)
      .errorIfExists(true)
      .compressionType(CompressionType.SNAPPY);

  private static final WriteOptions DEFAULT_WRITE_OPTIONS = new WriteOptions()
      .sync(true)
      .snapshot(false);

  private final DB plainDataBase;

  private int flushedPagesCount = 0;

  private WriteBatch writeBatch = null;
  private int pagesInBatch = 0;

  PlainPageBuilder(Path plainPagePath) throws IOException {
    plainDataBase = JniDBFactory.factory.open(plainPagePath.toFile(), DEFAULT_DB_OPTIONS);
  }

  int add(CrawlerDocument newPage) {
    if (writeBatch == null) {
      writeBatch = plainDataBase.createWriteBatch();
    }

    if (pagesInBatch >= MAX_PAGES_IN_BATCH) {
      plainDataBase.write(writeBatch, DEFAULT_WRITE_OPTIONS);
      pagesInBatch = 0;
      writeBatch = plainDataBase.createWriteBatch();
    }

    byte[] pageIdBytes = ByteBuffer.allocate(4).putInt(flushedPagesCount).array();
    byte[] pageBytes = IndexUnits.Page
        .newBuilder()
        .setPageId(flushedPagesCount)
        .setContent(newPage.getContent().toString())
        .setTitle(newPage.getTitle())
        .build()
        .toByteArray();

    writeBatch.put(pageIdBytes, pageBytes);
    ++pagesInBatch;

    return flushedPagesCount++;
  }

  void build() throws IOException {
    if (pagesInBatch > 0) {
      plainDataBase.write(writeBatch, DEFAULT_WRITE_OPTIONS);
      pagesInBatch = 0;
      writeBatch = plainDataBase.createWriteBatch();
    }

    plainDataBase.close();
  }
}
