package com.expleague.sensearch.donkey.writers;

import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.nio.file.Path;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TermStatisticsWriter implements Writer<TermStatistics> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PageWriter.class);
  private static final Options DB_OPTIONS = new Options()
      .blockSize(1 << 20)
      .cacheSize(1 << 20)
      .createIfMissing(true)
      .writeBufferSize(1 << 10);

  private static final int BATCH_SIZE = 1024;

  private WriteBatch writeBatch;
  private int batchSize;

  private final Path root;
  private final DB wordStatsDb;

  public TermStatisticsWriter(Path outputPath) {
    this.root = outputPath;
    try {
      wordStatsDb = JniDBFactory.factory.open(outputPath.toFile(), DB_OPTIONS);
      writeBatch = wordStatsDb.createWriteBatch();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    wordStatsDb.close();
  }

  @Override
  public void flush() {
    try {
      wordStatsDb.write(writeBatch);
      writeBatch.close();
      writeBatch = wordStatsDb.createWriteBatch();
      batchSize = 0;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

