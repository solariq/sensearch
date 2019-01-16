package com.expleague.sensearch.utils;

import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.donkey.plain.IdGenerator;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class IndexBasedTestCase extends CrawlerBasedTestCase {

  protected static final String MINI_INDEX_ROOT = "MiniIndex";

  // TODO: prepare data for logs based lemmer!
  private static final String LOG_BASED_LEMMER_ROOT = "LemmerData";

  private static final Logger LOG = LoggerFactory.getLogger(IndexBasedTestCase.class);

  private static Index miniIndex;

  private static TestConfigImpl indexConfig;

  @BeforeClass
  public static void initIndex() throws IOException {
    Path indexRoot = testDataRoot().resolve(MINI_INDEX_ROOT);
    indexConfig = crawlerConfig().setTemporaryIndex(indexRoot);
    if (!Files.exists(indexRoot)) {
      LOG.info(String.format("Index was not found by given path [%s]. Index will be rebuilt",
          indexRoot.toAbsolutePath().toString())
      );
      buildIndex();
    }

    miniIndex = new PlainIndex(indexConfig);
  }

  private static void buildIndex() throws IOException {
    LOG.info("Rebuilding index...");
    MyStem myStem = myStemForTest("IndexBasedTestCase", "initIndex");
    new PlainIndexBuilder(crawler(), indexConfig, new Lemmer(myStem), new IdGenerator())
        .buildIndex();
  }

  protected static TestConfigImpl indexConfig() {
    return new TestConfigImpl(indexConfig);
  }

  protected static Index index() {
    return miniIndex;
  }

  protected ConfigImpl config() {
    return new TestConfigImpl(indexConfig);
  }
}
