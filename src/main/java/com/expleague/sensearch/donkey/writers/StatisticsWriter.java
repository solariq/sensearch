package com.expleague.sensearch.donkey.writers;

import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.google.common.primitives.Ints;
import java.io.IOException;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;

public class StatisticsWriter implements Writer<TermStatistics> {

  private static final int BATCH_SIZE = 1024;
  private final DB db;

  private WriteBatch writeBatch;
  private int batchSize;

  public StatisticsWriter(DB db) {
    this.db = db;
    writeBatch = db.createWriteBatch();
  }

  @Override
  public void write(TermStatistics stats) {
    writeBatch.put(Ints.toByteArray(stats.getTermId()), stats.toByteArray());
    batchSize++;
    if (batchSize >= BATCH_SIZE) {
      flush();
    }
  }

  @Override
  public void close() throws IOException {
    flush();
    db.close();
  }

  @Override
  public void flush() {
    try {
      db.write(writeBatch);
      writeBatch.close();
      writeBatch = db.createWriteBatch();
      batchSize = 0;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

