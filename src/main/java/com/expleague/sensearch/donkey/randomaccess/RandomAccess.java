package com.expleague.sensearch.donkey.randomaccess;

import java.io.Closeable;

public interface RandomAccess<Key, T> extends Iterable<T>, Closeable {

  T value(Key id);

  void put(Key id, T value);
}
