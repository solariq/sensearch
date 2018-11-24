package com.expleague.sensearch.utils;

import com.expleague.sensearch.index.Index;
import org.junit.BeforeClass;

public abstract class IndexBasedTestCase extends SensearchTestCase {

  private static Index miniIndex;

  @BeforeClass
  public static void initIndex() {

  }

  protected static Index index() {
    return miniIndex;
  }
}
