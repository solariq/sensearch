package com.expleague.sensearch.donkey.randomaccess;

import com.google.common.primitives.Longs;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Consumer;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TODO: support batch reading
public abstract class LevelDbBasedIndex<T> implements Iterable<T>, Closeable, AutoCloseable {
  static final Options DB_OPTIONS = new Options()
      .createIfMissing(false)
      .cacheSize(1 << 25)
      .errorIfExists(false);
  final Path root;
  final DB dataBase;

  public LevelDbBasedIndex(Path root) {
    this.root = root;
    try {
      dataBase = JniDBFactory.factory.open(root.toFile(), DB_OPTIONS);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  public T getValue(long id) {
    byte[] bytes = dataBase.get(Longs.toByteArray(id));
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    return decodeValue(bytes);
  }

  public void put(long id, T value) {
    // TODO: support operation
    throw new UnsupportedOperationException();
  }

  protected abstract T decodeValue(byte[] bytes);

  protected abstract byte[] encodeValue(T value);

  @NotNull
  @Override
  public Iterator<T> iterator() {
    return new DbIteratorWrapper();
  }

  @Override
  public void forEach(Consumer<? super T> consumer) {
    DBIterator iterator = dataBase.iterator();
    iterator.seekToFirst();
    iterator.forEachRemaining(e -> consumer.accept(decodeValue(e.getValue())));
  }

  @Override
  public void close() throws IOException {
    dataBase.close();
  }

  public class DbIteratorWrapper implements Iterator<T> {
    private final DBIterator dbIterator;
    private DbIteratorWrapper() {
      dbIterator = dataBase.iterator();
      dbIterator.seekToFirst();
    }

    @Override
    public boolean hasNext() {
      return dbIterator.hasNext();
    }

    @Override
    public T next() {
      return decodeValue(dbIterator.next().getValue());
    }

    @Override
    public void forEachRemaining(Consumer<? super T> consumer) {
      dbIterator.forEachRemaining(e -> consumer.accept(decodeValue(e.getValue())));
    }
  }
}
