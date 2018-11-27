package com.expleague.sensearch.utils;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.index.Index;
import org.junit.BeforeClass;

public abstract class IndexBasedTestCase extends SensearchTestCase {

  private static final String MINI_INDEX_ROOT = "MINI_INDEX";

  private static Index miniIndex;

  @BeforeClass
  public static void initIndex() {

  }
  protected Config config() {
    return null;
  }

  protected static Index index() {
    return miniIndex;
  }
}
