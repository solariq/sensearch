package com.expleague.sensearch.donkey.crawler.document;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    Assert.assertEquals(page.iD(), 6673504);
    Assert.assertEquals(page.title(), "Тэмусин (Когурё)»");
    Assert.assertEquals(page.sections().get(0).title(), Collections.singletonList("Тэмусин (Когурё)»"));
    Assert.assertEquals(page.sections().size(), 1);
    Assert.assertEquals(page.sections().get(0).links().size(), 0);
    Assert.assertEquals(page.sections().get(0).text().toString(),
        "прислал армию, захватившую Наннан в 44 году.\n"
            + "\n"
            + "Так же и по сей день хорошо известна легендарная история любви князя Хобона и принцессы Наннан (Nangnang). Принцесса, как говорят, разорвала военные барабаны в своем замке, чтобы Когурё смог атаковать без предупреждения.");
  }

  @Test(expected = IllegalArgumentException.class)
  public void brokenXMLTest() {
    page = parser.parseXML(
        Paths.get("./src/test/java/com/expleague/sensearch/donkey/crawler/resources/BrokenXML")
            .toFile());
  }

  @Test
  public void xMLWithEmptyLinksTest() {
    page = parser.parseXML(
        Paths.get("./src/test/java/com/expleague/sensearch/donkey/crawler/resources/XMLwithEmptyLink")
            .toFile());
    Assert.assertEquals(page.iD(), 3666303);
    Assert.assertEquals(page.sections().get(0).links().size(), 0);
    Assert.assertEquals(page.sections().get(1).links().size(), 1);

    Link link = page.sections().get(1).links().get(0);
    Assert.assertEquals(link.targetId(), -1);
    Assert.assertEquals(link.textOffset(), 0);
    Assert.assertEquals(link.text().toString(), " ");
    Assert.assertEquals(link.targetTitle().toString(), "");
  }

  @Test
  public void xMLwithEmptySectionsTest() {
    page = parser.parseXML(Paths.get(
        "./src/test/java/com/expleague/sensearch/donkey/crawler/resources/XMLwithEmptySections")
        .toFile());
    Assert.assertEquals(page.iD(), 6676369);
    Assert.assertEquals(page.title(), "Мужун Вэй");
    Assert.assertEquals(page.sections().size(), 6);

    Assert.assertEquals(page.sections().get(2).title(), Collections.singletonList("test"));
    Assert.assertEquals(page.sections().get(2).text().toString(), "");

    Assert.assertEquals(page.sections().get(0).title(), Collections.singletonList("Мужун Вэй"));
    //Assert.assertEquals(page.sections().get(0).text().toString(),
        //"");

  }

  @Test
  public void xMLwithoutCategoriesTest() {
    page = parser.parseXML(Paths.get(
        "./src/test/java/com/expleague/sensearch/donkey/crawler/resources/XMLwithoutCategories")
        .toFile());
    Assert.assertEquals(page.categories().size(), 0);

    Assert.assertEquals(page.iD(), 6673602);
    Assert.assertEquals(page.title(), "Ги V (виконт Лиможа)");
    Assert.assertEquals(page.sections().size(), 2);

    Assert.assertEquals(page.sections().get(0).title(), Collections.singletonList("Ги V (виконт Лиможа)"));
    List<CharSequence> test = new ArrayList<>();
    test.add("Ги V (виконт Лиможа)");
    test.add("Ссылки");
    Assert.assertEquals(page.sections().get(1).title(), test);
  }

  @Test
  public void xMLwithoutIDTest() {
    page = parser.parseXML(Paths.get(
        "./src/test/java/com/expleague/sensearch/donkey/crawler/resources/XMLwithoutID")
        .toFile());
    Assert.assertEquals(page.iD(), 0);

    Assert.assertEquals(page.title(), "Тэсо (Тонбуё)");
    Assert.assertEquals(page.sections().size(), 1);

    Assert.assertEquals(page.sections().get(0).title(), Collections.singletonList("Тэсо (Тонбуё)"));
  }

  @Test
  public void xMLwithoutTitleTest() {
    page = parser.parseXML(Paths.get(
        "./src/test/java/com/expleague/sensearch/donkey/crawler/resources/XMLwithoutTitle")
        .toFile());
    List<CharSequence> test = new ArrayList<>();
    test.add("Императоры Поздней Чжао");
    test.add("Правители Азии IV века");
    Assert.assertEquals(page.iD(), 6673315);
    Assert.assertNull(page.title());
    Assert.assertEquals(page.sections().size(), 3);
    Assert.assertEquals(page.categories(), test);
    Assert.assertEquals(page.sections().get(0).title(), Collections.singletonList("Ши Чжи"));
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

  @Test
  public void xMLWithoutLinks() {
    page = parser.parseXML(Paths.get(
        "./src/test/java/com/expleague/sensearch/donkey/crawler/resources/XMLWithoutLinks")
        .toFile());
    Assert.assertEquals(page.iD(), 6675547);
    Assert.assertEquals(page.title(), "Бонифаций дель Васто");
    Assert.assertEquals(page.sections().size(), 3);
    Assert.assertEquals(page.sections().get(2).text().toString(),
        "C.W. Previté-Orton, The Early History of the House of Savoy (1000—1233) (Cambridge, 1912), Malaterra, De rebus gestis Rogerii Calabriæ et Siciliæ comitis et Roberti Guiscardi ducis fratris eius, ed. E. Pontiari, Rerum Italicarum Scriptores, nuova ed. v. 5.1 (Bolgna, 1927—1928). R. Bordone, ‘Affermazione personale e sviluppi dinastici del gruppo parentalae aleramico: il marchese Bonifacio ‘del Vasto’,’ in Formazione e strutture dei ceti dominanti nel medioevo (Atti del I convegno di Pisa: 10-11 maggio 1983) (Rome, 1988), pp. 29-44. L. Provero, Dai marchesi del Vasto ai primi marchesi di Saluzzo. Sviluppi signorili entro quadri pubblici (secoli XI—XII) (Turin, 1992). Giuseppe Sorge, Mussomeli dall’origine all’abolizione della feudalità, vol. II, Catania 1916, poi Edizioni Ristampe Siciliane, Palermo 1982. http://fmg.ac/Projects/MedLands/MONFERRATO,%20SALUZZO,%20SAVONA.htm#_Toc359999490");
  }
}
