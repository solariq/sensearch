package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import com.expleague.sensearch.utils.SensearchTestCase;
import org.junit.Test;

public class CompleteIndexBuildTest extends SensearchTestCase {
  @Test
  public void completeBuildTest() {
    Crawler crawler = new CrawlerXML(config());
  }
}
