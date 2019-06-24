package com.expleague.sensearch.donkey.randomaccess;

import java.io.Closeable;

public interface RandomAccess<T> extends Iterable<T>, Closeable {
  T value(long id);
  void put(long id, T value);
}
