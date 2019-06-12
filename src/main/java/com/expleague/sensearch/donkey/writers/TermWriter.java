package com.expleague.sensearch.donkey.writers;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.nio.file.Path;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TermWriter implements Closeable, Flushable {
  private static final Logger LOGGER  = LoggerFactory.getLogger(PageWriter.class);
  private static final Options DB_OPTIONS = new Options()
      .blockSize(1 << 20)
      .cacheSize(1 << 20)
      .createIfMissing(true)
      .writeBufferSize(1 << 10);
  private static final WriteOptions WRITE_OPTIONS = new WriteOptions()
      .sync(true);
  private static final int BATCH_SIZE = 1000;

  private final DB termsDb;
  private final Path root;
  private WriteBatch writeBatch;
  public TermWriter(Path root) {
    this.root = root;
    try {
      termsDb = JniDBFactory.factory.open(root.toFile(), DB_OPTIONS);
      writeBatch = termsDb.createWriteBatch();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void writeTerm() {

  }

  @Override
  public void close() throws IOException {
    termsDb.write(writeBatch, WRITE_OPTIONS);
    termsDb.close();
  }

  @Override
  public void flush() throws IOException {
    termsDb.write(writeBatch, WRITE_OPTIONS);
    writeBatch = termsDb.createWriteBatch();
  }
}
