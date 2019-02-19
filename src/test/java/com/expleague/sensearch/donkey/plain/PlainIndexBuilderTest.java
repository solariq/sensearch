package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import com.expleague.sensearch.utils.SensearchTestCase;
import com.expleague.sensearch.utils.TestConfigImpl;
import org.junit.Before;
import org.junit.Test;

public class PlainIndexBuilderTest extends SensearchTestCase {

  private static final String INDEX_ROOT_NAME = "index";

  private Config config;

  @Before
  public void initConfigPaths() {
    this.config = config();
    ((TestConfigImpl) config).setTemporaryIndex(testOutputRoot().resolve(INDEX_ROOT_NAME));
  }

  @Test
  public void completeBuildTest() throws Exception {
    IndexBuilder indexBuilder =
        new PlainIndexBuilder(
            new CrawlerXML(config.getPathToZIP()),
            config.getIndexRoot(),
            config.getEmbeddingVectors(),
            new Lemmer(myStemForTest("PlainIndexBuilderTest", "completeBuildTest")));
    indexBuilder.buildIndexAndEmbedding();
  }
}
