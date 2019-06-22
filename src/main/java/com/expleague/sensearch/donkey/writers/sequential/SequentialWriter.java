package com.expleague.sensearch.donkey.writers.sequential;

import java.io.Closeable;

public interface SequentialWriter<T> extends Closeable {
  void append(T object);
}
