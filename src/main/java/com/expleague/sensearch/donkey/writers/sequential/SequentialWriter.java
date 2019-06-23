package com.expleague.sensearch.donkey.writers.sequential;

import com.expleague.sensearch.donkey.writers.Writer;

public interface SequentialWriter<T> extends Writer<T> {
  void append(T object);

  @Override
  default void write(T object) {
    append(object);
  }
}
