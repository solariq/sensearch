package com.expleague.sensearch.donkey.readers;

import java.io.Closeable;

public interface Reader<T> extends Closeable {
  T read();
}
