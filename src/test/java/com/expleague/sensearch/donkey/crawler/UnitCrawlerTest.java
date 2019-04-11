package com.expleague.sensearch.donkey.crawler;

import com.expleague.sensearch.experiments.wiki.CrawlerWiki;
import com.expleague.sensearch.utils.SensearchTestCase;
import com.expleague.sensearch.utils.TestConfigImpl;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import org.junit.Assert;
import org.junit.Test;

public class UnitCrawlerTest extends SensearchTestCase {


  private static final Path RESOURCES_ROOT = testDataRoot().resolve("CrawlerTestsData");

  private Crawler crawler;

  // TODO test sections when some intermediate sections are missing

  @Test
  public void goodZIPTest() throws IOException, XMLStreamException {
    TestConfigImpl config = sensearchConfig()
        .setPathToZIP(RESOURCES_ROOT.resolve("Mini_Wiki.zip"));
    crawler = new CrawlerWiki(config.getPathToZIP());

    Assert.assertEquals(crawler.makeStream().count(), 10);

    Set<String> titles = new HashSet<>();
    Set<String> rightTitles = new HashSet<>();
    rightTitles.add("Мужун Бао");
    rightTitles.add("Самохина, Дарья Сергеевна");
    rightTitles.add("Мужун Вэй");
    rightTitles.add("Бонифаций дель Васто");
    rightTitles.add("Ги V (виконт Лиможа)");
    rightTitles.add("Тэмусин (Когурё)»");
    rightTitles.add("Тэсо (Тонбуё)");
    rightTitles.add("Ши Чжи");
    rightTitles.add("Нифай");
    rightTitles.add("Итен");
    crawler
        .makeStream()
        .forEach(
            doc -> {
              if (doc != null) {
                titles.add(doc.title());
              }
            });
    Assert.assertEquals(titles, rightTitles);
  }

  @Test
  public void badZIPTest() throws IOException, XMLStreamException {
    TestConfigImpl config = sensearchConfig()
        .setPathToZIP(RESOURCES_ROOT.resolve("Mini_Wiki_broken.zip"));
    crawler = new CrawlerWiki(config.getPathToZIP());

    Assert.assertEquals(crawler.makeStream().count(), 10);

    Set<String> titles = new HashSet<>();
    Set<String> rightTitles = new HashSet<>();
    rightTitles.add("Мужун Бао");
    rightTitles.add("Самохина, Дарья Сергеевна");
    rightTitles.add("Бонифаций дель Васто");
    rightTitles.add("Тэмусин (Когурё)»");
    rightTitles.add("Тэсо (Тонбуё)");
    rightTitles.add("Ши Чжи");
    rightTitles.add("Итен");
    crawler
        .makeStream()
        .forEach(
            doc -> {
              if (doc != null) {
                titles.add(doc.title());
              }
            });
    Assert.assertEquals(titles, rightTitles);
  }
}
