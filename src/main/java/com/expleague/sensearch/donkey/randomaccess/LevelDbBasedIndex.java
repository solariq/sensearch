package com.expleague.sensearch.donkey.randomaccess;

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
public abstract class LevelDbBasedIndex<K, V> implements RandomAccess<K, V> {
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
  @Override
  public V value(K id) {
    byte[] bytes = dataBase.get(encodeKey(id));
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    return decodeValue(bytes);
  }

  @Override
  public void put(K id, V value) {
    byte[] keyBytes = encodeKey(id);
    byte[] valueBytes = encodeValue(value);
    dataBase.put(keyBytes, valueBytes);
  }

  protected abstract V decodeValue(byte[] bytes);

  protected abstract byte[] encodeValue(V value);

  protected abstract byte[] encodeKey(K key);

  @NotNull
  @Override
  public Iterator<V> iterator() {
    return new DbIteratorWrapper();
  }

  @Override
  public void forEach(Consumer<? super V> consumer) {
    DBIterator iterator = dataBase.iterator();
    iterator.seekToFirst();
    iterator.forEachRemaining(e -> consumer.accept(decodeValue(e.getValue())));
  }

  @Override
  public void close() throws IOException {
    dataBase.close();
  }

  public class DbIteratorWrapper implements Iterator<V> {
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
    public V next() {
      return decodeValue(dbIterator.next().getValue());
    }

    @Override
    public void forEachRemaining(Consumer<? super V> consumer) {
      dbIterator.forEachRemaining(e -> consumer.accept(decodeValue(e.getValue())));
    }
  }
}
