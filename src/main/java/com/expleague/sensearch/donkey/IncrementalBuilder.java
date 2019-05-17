package com.expleague.sensearch.donkey;

import java.util.ArrayList;
import java.util.List;

public interface IncrementalBuilder extends RecoverableBuilder {
  static <T extends BuilderState> List<T> accumulate(Class<T> stateClass,
      BuilderState ... states) {
    List<T> convertedStates = new ArrayList<>();
    for (BuilderState state : states) {
      try {
        convertedStates.add(stateClass.cast(state));
      } catch (ClassCastException e) {
        throw new IllegalArgumentException(
            String.format("One of the received states is not a state of %s",
                stateClass.getSimpleName()),
            e);
      }
    }
    return convertedStates;
  }
  void setStates(BuilderState... increments);
  default void setState(BuilderState state) {
    setStates(state);
  }
}
