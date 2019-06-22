package com.expleague.sensearch.donkey.writers;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

public interface Writer<T> extends Closeable, Flushable {
  public void write(T object) throws IOException;
}
