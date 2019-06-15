package com.expleague.sensearch.donkey.writers;

import com.expleague.sensearch.donkey.utils.BrandNewIdGenerator;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.nio.file.Path;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

public abstract class LevelDbBasedWriter implements Closeable, Flushable {

  static final Options DB_OPTIONS = new Options()
      .blockSize(1 << 20)
      .cacheSize(1 << 20)
      .createIfMissing(true)
      .writeBufferSize(1 << 10);
  static final WriteOptions WRITE_OPTIONS = new WriteOptions()
      .sync(true);
  static final int BATCH_SIZE = 1_000;


  final BrandNewIdGenerator idGenerator = BrandNewIdGenerator.getInstance();
  final DB rootDb;
  final Path root;
  WriteBatch writeBatch;

  public LevelDbBasedWriter(Path root) {
    this.root = root;
    try {
      rootDb = JniDBFactory.factory.open(root.toFile(), DB_OPTIONS);
      writeBatch = rootDb.createWriteBatch();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws IOException {
    rootDb.write(writeBatch, WRITE_OPTIONS);
    rootDb.close();
  }

  @Override
  public void flush() {
    rootDb.write(writeBatch, WRITE_OPTIONS);
    writeBatch = rootDb.createWriteBatch();
  }
}
