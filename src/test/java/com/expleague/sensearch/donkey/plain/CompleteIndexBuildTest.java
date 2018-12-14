package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.utils.SensearchTestCase;
import com.expleague.sensearch.utils.TestConfigImpl;
import org.junit.Before;
import org.junit.Test;

public class CompleteIndexBuildTest extends SensearchTestCase {

  private static String INDEX_ROOT_NAME = "index";
  private static String TEMP_DOC_PATH = "tempDoc";

  private Config config;

  @Before
  public void initConfigPaths() {
    this.config = config();
    ((TestConfigImpl) config).setTemporaryIndex(testOutputRoot().resolve(INDEX_ROOT_NAME));
    ((TestConfigImpl) config).setTemporaryDocuments(testOutputRoot().resolve(TEMP_DOC_PATH));
  }

  @Test
  public void completeBuildTest() throws Exception {
//    IndexBuilder indexBuilder = new PlainIndexBuilder(new CrawlerXML(config), config, lemmer);
//    indexBuilder.buildIndex(new CrawlerXML(config), config, lemmer);
  }
}
