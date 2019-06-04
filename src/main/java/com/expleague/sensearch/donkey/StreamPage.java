package com.expleague.sensearch.donkey;

import java.util.stream.IntStream;

public interface StreamPage {

  /**
   * Utility class, just syntax sugar
   * @return {@link IntStream}
   */
  IntStream stream();

}
