package com.expleague.sensearch.donkey.writers;

import com.expleague.sensearch.donkey.utils.BrandNewIdGenerator;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import java.io.IOException;
import java.nio.file.Path;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: make thread safe
public class LevelDbLinkWriter implements Writer<Page.Link> {

  private static final Logger LOGGER = LoggerFactory.getLogger(LevelDbLinkWriter.class);

  private static final Options DB_OPTIONS = new Options()
      .blockSize(1 << 20)
      .cacheSize(1 << 20)
      .createIfMissing(true)
      .writeBufferSize(1 << 10);
  private static final WriteOptions WRITE_OPTIONS = new WriteOptions()
      .sync(true);
  private static final int BATCH_SIZE = 1000;
  private static final byte[] EMPTY_VALUE = new byte[0];

  private final Path root;
  private final DB linksDb;
  private final BrandNewIdGenerator idGenerator = BrandNewIdGenerator.getInstance();
  private WriteBatch writeBatch;

  public LevelDbLinkWriter(Path outputPath) {
    this.root = outputPath;
    try {
      linksDb = JniDBFactory.factory.open(outputPath.toFile(), DB_OPTIONS);
      writeBatch = linksDb.createWriteBatch();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void write(Page.Link link) {
    writeBatch.put(link.toByteArray(), EMPTY_VALUE);
    flush();
  }

  @Override
  public void close() throws IOException {
    linksDb.write(writeBatch, WRITE_OPTIONS);
    linksDb.close();
  }

  @Override
  public void flush() {
    linksDb.write(writeBatch, WRITE_OPTIONS);
    writeBatch = linksDb.createWriteBatch();
  }
}
