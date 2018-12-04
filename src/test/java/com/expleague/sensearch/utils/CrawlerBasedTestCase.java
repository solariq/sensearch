package com.expleague.sensearch.utils;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CrawlerBasedTestCase extends SensearchTestCase {
  private static final Logger LOG = LoggerFactory.getLogger(CrawlerBasedTestCase.class);

  private static Crawler miniCrawler;

  private static TestConfig crawlerConfig;

  @BeforeClass
  public static void initCrawler() {
    Path miniWikiRoot = miniWikiRoot();
    if (Files.notExists(miniWikiRoot)) {
      LOG.warn(String.format("Raw data was not found by given path [%s]",
          miniWikiRoot.toAbsolutePath().toString())
      );
      LOG.warn("Crawler was not initialized!");
      // TODO: Fail with exception? Probably tests taht require crawler should be skipped in that matter...
      return;
    }
    crawlerConfig = sensearchConfig()
        .setTemporaryDocuments(testOutputRoot().resolve("CrawlerTempDocs"));
    miniCrawler = new CrawlerXML(crawlerConfig);
  }

  protected static TestConfig crawlerConfig() {
    return new TestConfig(crawlerConfig);
  }

  protected static Crawler crawler() {
    return miniCrawler;
  }

  protected Config config() {
    return new TestConfig(crawlerConfig);
  }
}
