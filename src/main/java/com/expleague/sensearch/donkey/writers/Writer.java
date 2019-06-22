package com.expleague.sensearch.donkey.writers;

import java.io.Closeable;
import java.io.Flushable;

public interface Writer<T> extends Closeable, Flushable {
  void write(T object);
}
