package com.expleague.sensearch.utils;

import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
  public static void main(String[] args) throws Exception {
    Path minWikiPath = Paths.get("./src/test/DATA/ForIndex/MiniWiki.zip");
    TestConfig config = new TestConfig()
        .setPathToZIP(minWikiPath)
        .setTemporaryDocuments(Paths.get("./src/test/UNIVERSE/TempDocs"));

    Crawler crawler = new CrawlerXML(config);
    crawler.makeStream().limit(3).forEach(d -> {System.out.println(d.title());});
  }
}
