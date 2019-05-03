package com.expleague.sensearch.donkey;

public interface IncrementalBuilder extends RecoverableBuilder {
  void increment(BuilderState... increments);
}
