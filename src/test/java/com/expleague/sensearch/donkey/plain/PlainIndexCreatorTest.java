package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.lemmer.MultiLangLemmer;
import com.expleague.sensearch.donkey.IndexCreator;
import com.expleague.sensearch.experiments.wiki.CrawlerWiki;
import com.expleague.sensearch.utils.SensearchTestCase;
import com.expleague.sensearch.utils.TestConfigImpl;
import org.junit.Before;
import org.junit.Test;

public class PlainIndexCreatorTest extends SensearchTestCase {

  private static final String INDEX_ROOT_NAME = "index";

  private Config config;

  @Before
  public void initConfigPaths() {
    this.config = config();
    ((TestConfigImpl) config).setTemporaryIndex(testOutputRoot().resolve(INDEX_ROOT_NAME));
  }

  @Test
  public void completeBuildTest() throws Exception {
    IndexCreator indexCreator =
        new PlainIndexCreator(
            new CrawlerWiki(config.getPathToZIP()),
            config.getIndexRoot(),
            config.getEmbeddingVectors(),
                MultiLangLemmer.getInstance());
    indexCreator.createWordEmbedding();
    indexCreator.createPagesAndTerms();
    indexCreator.createLinks();
    indexCreator.createStats();
    indexCreator.createPageEmbedding();
    indexCreator.createSuggest();
  }
}
