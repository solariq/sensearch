package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import com.expleague.sensearch.utils.SensearchTestCase;
import com.expleague.sensearch.utils.TestConfig;
import org.junit.Before;
import org.junit.Test;

public class CompleteIndexBuildTest extends SensearchTestCase {

  private static String INDEX_ROOT_NAME = "index";
  private static String TEMP_DOC_PATH = "tempDoc";

  private Config config;
  @Before
  public void initConfigPaths() {
    this.config = config();
    ((TestConfig) config).setIndexRoot(testOutputRoot().resolve(INDEX_ROOT_NAME));
    ((TestConfig) config).setTempDocumentsPath(testOutputRoot().resolve(TEMP_DOC_PATH));
  }

  @Test
  public void completeBuildTest() throws Exception {
    IndexBuilder indexBuilder = new PlainIndexBuilder();
    indexBuilder.buildIndex(new CrawlerXML(config), config);
  }
}
