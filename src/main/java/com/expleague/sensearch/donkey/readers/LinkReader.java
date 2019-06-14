package com.expleague.sensearch.donkey.readers;

import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.Link;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import org.fusesource.leveldbjni.JniDBFactory;
import org.fusesource.leveldbjni.internal.JniDB;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

public class LinkReader implements Closeable, AutoCloseable {

  private static final Options DB_OPTIONS = new Options()
      .createIfMissing(false)
      .cacheSize(1 << 25)
      .errorIfExists(false);
  private final Path root;
  private final DB linkDb;
  private final DBIterator iterator;

  public LinkReader(Path root) {
    this.root = root;
    try {
      linkDb = JniDBFactory.factory.open(root.toFile(), DB_OPTIONS);
      iterator = linkDb.iterator();
      iterator.seekToFirst();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Link read() {
    if (iterator.hasNext()) {
      try {
        return Page.Link.parseFrom(iterator.next().getKey());
      } catch (InvalidProtocolBufferException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  @Override
  public void close() throws IOException {
    linkDb.close();
  }
}
