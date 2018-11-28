package com.expleague.sensearch.donkey.crawler.document;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
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
    Assert.assertEquals(page.sections().get(0).title(), Collections.singletonList("Ссылки"));
    Assert.assertEquals(page.sections().size(), 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void brokenXMLTest() {
    page = parser.parseXML(
        Paths.get("./src/test/java/com/expleague/sensearch/donkey/crawler/resources/BrokenXML")
            .toFile());
  }

  @Test
  public void xMLwithEmptySectionsTest() {
    page = parser.parseXML(Paths.get(
        "./src/test/java/com/expleague/sensearch/donkey/crawler/resources/XMLwithEmptySections")
        .toFile());
    Assert.assertEquals(page.iD(), 3457253);
    Assert.assertEquals(page.title(), "Нагуманов, Андрей Рафаилович");
    Assert.assertEquals(page.sections().size(), 7);

    Assert.assertEquals(page.sections().get(2).title(), Collections.singletonList("Личная жизнь"));
    Assert.assertEquals(page.sections().get(2).text().toString(),
        "Старший брат Роман — также футболист.\n"
            + "\n"
            + "Жена родилась в Петербурге, с 8 лет живёт в Германии. Нагуманов встретил её в 2006 году, женился в 2013, тогда же они переехали в Германию.");

    Assert.assertEquals(page.sections().get(3).title(), Collections.singletonList("test"));
    Assert.assertEquals(page.sections().get(3).text().toString(), "");

    Assert.assertEquals(page.sections().get(4).title(), Collections.singletonList("Личная жизнь"));
    Assert.assertEquals(page.sections().get(4).text().toString(), "");

    Assert.assertEquals(page.sections().get(5).title(), Collections.singletonList("Личная жизнь"));
    Assert.assertEquals(page.sections().get(5).text().toString(), "");

    Assert.assertEquals(page.sections().get(6).title(), Collections.singletonList("Личная жизнь"));
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

    Assert.assertEquals(page.sections().get(2).title(), Collections.singletonList("Личная жизнь"));
  }

  @Test
  public void xMLwithoutIDTest() {
    page = parser.parseXML(Paths.get(
        "./src/test/java/com/expleague/sensearch/donkey/crawler/resources/XMLwithoutID")
        .toFile());
    Assert.assertEquals(page.iD(), 0);

    Assert.assertEquals(page.title(), "Стела «Город воинской славы» (Курск)");
    Assert.assertEquals(page.sections().size(), 3);

    Assert.assertEquals(page.sections().get(2).title(), Collections.singletonList("Галерея"));
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

  @Test
  public void xmlWithSubcategoriesTest() {
    page = parser.parseXML(Paths.get(
        "./src/test/java/com/expleague/sensearch/donkey/crawler/resources/XMLWithSubsections")
        .toFile());

    Assert.assertEquals(5, page.sections().size());
    Assert.assertEquals(Arrays.asList("Ссылки"), page.sections().get(0).title());
    Assert.assertEquals(Arrays.asList("Ссылки", "Ссылка1"), page.sections().get(1).title());
    Assert.assertEquals(Arrays.asList("Ссылки", "Ссылка2"), page.sections().get(2).title());
    Assert.assertEquals(
        Arrays.asList("Ссылки", "Ссылка2", "Подссылка"), page.sections().get(3).title());
    Assert.assertEquals(Arrays.asList("Не Ссылки"), page.sections().get(4).title());
  }

  @Test
  public void xmlWithLinksTest() {
    page = parser.parseXML(Paths.get(
        "./src/test/java/com/expleague/sensearch/donkey/crawler/resources/XMLWithLinks")
        .toFile());
    Assert.assertEquals(3, page.sections().size());
    Assert.assertEquals(2, page.sections().get(0).links().size());

    Assert.assertEquals("точимилькококо",
        page.sections().get(0).links().get(0).targetTitle());
    Assert.assertEquals(123, page.sections().get(0).links().get(0).targetId());
    Assert.assertEquals(0, page.sections().get(0).links().get(0).textOffset());

    Assert.assertEquals("ПуэблА",
        page.sections().get(0).links().get(1).targetTitle());
    Assert.assertEquals(-1, page.sections().get(0).links().get(1).targetId());
    Assert.assertEquals(44, page.sections().get(0).links().get(1).textOffset());


    Assert.assertEquals("",
        page.sections().get(2).links().get(0).targetTitle());
    Assert.assertEquals(-1, page.sections().get(2).links().get(0).targetId());
    Assert.assertEquals(0, page.sections().get(2).links().get(0).textOffset());
  }
}
