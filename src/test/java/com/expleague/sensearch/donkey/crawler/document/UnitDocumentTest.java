package com.expleague.sensearch.donkey.crawler.document;

import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Test;

public class UnitDocumentTest {

  private XMLParser parser = new XMLParser();
  private CrawlerDocument page;


  @Test
  public void smallXMLTest() {
    page = parser.parseXML(
        Paths.get("./src/test/java/com/expleague/sensearch/donkey/crawler/resources/smallXML")
            .toFile());
    Assert.assertEquals(page.iD(), 1105826);
    Assert.assertEquals(page.title(), "Точимилько");
    Assert.assertEquals(page.sections().get(0).title(), "Ссылки");
    Assert.assertEquals(page.sections().size(), 1);
  }

  @Test
  public void brokenXMLTest() {
    page = parser.parseXML(
        Paths.get("./src/test/java/com/expleague/sensearch/donkey/crawler/resources/BrokenXML")
            .toFile());
    Assert.assertNull(page);
  }

  @Test
  public void xMLwithEmptySectionsTest() {
    page = parser.parseXML(Paths.get(
        "./src/test/java/com/expleague/sensearch/donkey/crawler/resources/XMLwithEmptySections")
        .toFile());
    Assert.assertEquals(page.iD(), 3457253);
    Assert.assertEquals(page.title(), "Нагуманов, Андрей Рафаилович");
    Assert.assertEquals(page.sections().size(), 7);

    Assert.assertEquals(page.sections().get(2).title().toString(), "Личная жизнь");
    Assert.assertEquals(page.sections().get(2).text().toString(),
        "Старший брат Роман — также футболист.\n"
            + "\n"
            + "Жена родилась в Петербурге, с 8 лет живёт в Германии. Нагуманов встретил её в 2006 году, женился в 2013, тогда же они переехали в Германию.");

    Assert.assertEquals(page.sections().get(3).title().toString(), "test");
    Assert.assertEquals(page.sections().get(3).text().toString(), "");

    Assert.assertEquals(page.sections().get(4).title().toString(), "Личная жизнь");
    Assert.assertEquals(page.sections().get(4).text().toString(), "");

    Assert.assertEquals(page.sections().get(5).title().toString(), "Личная жизнь");
    Assert.assertEquals(page.sections().get(5).text().toString(), "");

    Assert.assertEquals(page.sections().get(6).title().toString(), "Личная жизнь");
    Assert.assertEquals(page.sections().get(6).text().toString(), "");
  }

  @Test
  public void xMLwithoutCategoriesTest() {
    page = parser.parseXML(Paths.get(
        "./src/test/java/com/expleague/sensearch/donkey/crawler/resources/XMLwithoutCategories")
        .toFile());
    Assert.assertEquals(page.categories().size(), 0);

    Assert.assertEquals(page.iD(), 3457253);
    Assert.assertEquals(page.title(), "Нагуманов, Андрей Рафаилович");
    Assert.assertEquals(page.sections().size(), 3);

    Assert.assertEquals(page.sections().get(2).title().toString(), "Личная жизнь");
  }

  @Test
  public void xMLwithoutIDTest() {
    page = parser.parseXML(Paths.get(
        "./src/test/java/com/expleague/sensearch/donkey/crawler/resources/XMLwithoutID")
        .toFile());
    Assert.assertEquals(page.iD(), 0);

    Assert.assertEquals(page.title(), "Стела «Город воинской славы» (Курск)");
    Assert.assertEquals(page.sections().size(), 3);

    Assert.assertEquals(page.sections().get(2).title().toString(), "Галерея");
  }

  @Test
  public void xMLwithoutTitleTest() {
    page = parser.parseXML(Paths.get(
        "./src/test/java/com/expleague/sensearch/donkey/crawler/resources/XMLwithoutTitle")
        .toFile());
    Assert.assertEquals(page.iD(), 3457253);
    Assert.assertNull(page.title());
    Assert.assertEquals(page.sections().size(), 3);
  }
}
