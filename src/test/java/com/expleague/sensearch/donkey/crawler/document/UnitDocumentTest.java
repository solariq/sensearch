package com.expleague.sensearch.donkey.crawler.document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Section;
import com.expleague.sensearch.experiments.wiki.XMLParser;
import com.expleague.sensearch.utils.SensearchTestCase;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class UnitDocumentTest extends SensearchTestCase {

  private static final Path RESOURCES_ROOT = testDataRoot().resolve("CrawlerTestsData");

  private XMLParser parser = new XMLParser();
  private CrawlerDocument page;

  @Test
  public void smallXMLTest() {
    page = parser.parseXML(RESOURCES_ROOT.resolve("smallXML").toFile());
    List<Section> sections = page.sections().collect(Collectors.toList());
//    assertEquals(page.id(), 6673504);
    assertEquals(page.title(), "Тэмусин (Когурё)»");
    assertEquals(sections.get(0).titles(), Collections.singletonList("Тэмусин (Когурё)»"));
    assertEquals(sections.size(), 1);
    assertEquals(sections.get(0).links().size(), 0);
    assertEquals(
        sections.get(0).text().toString(),
        "прислал армию, захватившую Наннан в 44 году.\n"
            + "\n"
            + "Так же и по сей день хорошо известна легендарная история любви князя Хобона и принцессы Наннан (Nangnang). Принцесса, как говорят, разорвала военные барабаны в своем замке, чтобы Когурё смог атаковать без предупреждения.");
  }

  @Test(expected = RuntimeException.class)
  public void brokenXMLTest() {
    page = parser.parseXML(RESOURCES_ROOT.resolve("BrokenXML").toFile());
  }

  @Test
  public void xMLWithEmptyLinksTest() {
    page = parser.parseXML(RESOURCES_ROOT.resolve("XMLwithEmptyLink").toFile());
    List<Section> sections = page.sections().collect(Collectors.toList());
//    assertEquals(page.id(), 3666303);
    assertEquals(sections.get(0).links().size(), 0);
    assertEquals(sections.get(1).links().size(), 1);

    Link link = sections.get(1).links().get(0);
//    assertEquals(link.targetId(), -1);
    assertEquals(link.textOffset(), 0);
    assertEquals(link.text().toString(), " ");
    assertEquals(link.targetTitle().toString(), "");
  }

  @Test
  public void xMLwithEmptySectionsTest() {
    page = parser.parseXML(RESOURCES_ROOT.resolve("XMLwithEmptySections").toFile());
    List<Section> sections = page.sections().collect(Collectors.toList());
//    assertEquals(page.id(), 6676369);
    assertEquals(page.title(), "Мужун Вэй");
    assertEquals(7, sections.size());

    assertEquals(sections.get(0).titles(), Collections.singletonList("Мужун Вэй"));
    assertEquals(
        "Мужун Вэй (, 350—385), взрослое имя Цзинмао (景茂) — сяньби йский вождь, последний император государства Ранняя Янь . От императора Южной Янь Мужун Дэ , который был его дядей, впоследствии получил посмертное имя Ю-ди (幽帝).",
        sections.get(0).text().toString());
  }

  @Test
  public void xMLwithoutCategoriesTest() {
    page = parser.parseXML(RESOURCES_ROOT.resolve("XMLwithoutCategories").toFile());
    assertEquals(page.categories().size(), 0);

    List<Section> sections = page.sections().collect(Collectors.toList());
//    assertEquals(page.id(), 6673602);
    assertEquals(page.title(), "Ги V (виконт Лиможа)");
    assertEquals(sections.size(), 2);

    assertEquals(sections.get(0).titles(), Collections.singletonList("Ги V (виконт Лиможа)"));
    List<CharSequence> test = new ArrayList<>();
    test.add("Ги V (виконт Лиможа)");
    test.add("Ссылки");
    assertEquals(sections.get(1).titles(), test);
  }

  @Test
  public void xMLwithoutIDTest() {
    page = parser.parseXML(RESOURCES_ROOT.resolve("XMLwithoutID").toFile());
//    assertEquals(page.id(), 0);

    List<Section> sections = page.sections().collect(Collectors.toList());
    assertEquals(page.title(), "Тэсо (Тонбуё)");
    assertEquals(sections.size(), 1);

    assertEquals(sections.get(0).titles(), Collections.singletonList("Тэсо (Тонбуё)"));
  }

  @Test
  public void xMLwithoutTitleTest() {
    page = parser.parseXML(RESOURCES_ROOT.resolve("XMLwithoutTitle").toFile());
    List<CharSequence> test = new ArrayList<>();
    test.add("Императоры Поздней Чжао");
    test.add("Правители Азии IV века");
//    assertEquals(page.id(), 6673315);
    assertEquals("", page.title());

    List<Section> sections = page.sections().collect(Collectors.toList());
    assertEquals(sections.size(), 3);
    assertEquals(page.categories(), test);
    assertEquals(sections.get(0).titles(), Collections.singletonList("Ши Чжи"));
  }

  @Test
  public void xmlWithSubcategoriesTest() {
    page = parser.parseXML(RESOURCES_ROOT.resolve("XMLWithSubsections").toFile());

    List<Section> sections = page.sections().collect(Collectors.toList());
    assertEquals(5, sections.size());
    assertEquals(Collections.singletonList("Ссылки"), sections.get(0).titles());
    assertEquals(Arrays.asList("Ссылки", "Ссылка1"), sections.get(1).titles());
    assertEquals(Arrays.asList("Ссылки", "Ссылка2"), sections.get(2).titles());
    assertEquals(Arrays.asList("Ссылки", "Ссылка2", "Подссылка"), sections.get(3).titles());
    assertEquals(Collections.singletonList("Не Ссылки"), sections.get(4).titles());
  }

  @Test
  public void xmlWithLinksTest() {
    page = parser.parseXML(RESOURCES_ROOT.resolve("XMLWithLinks").toFile());

    List<Section> sections = page.sections().collect(Collectors.toList());
    assertEquals(3, sections.size());
    assertEquals(2, sections.get(0).links().size());

    assertEquals("точимилькококо", sections.get(0).links().get(0).targetTitle());
//    assertEquals(123, sections.get(0).links().get(0).targetId());
    assertEquals(0, sections.get(0).links().get(0).textOffset());

    assertEquals("ПуэблА", sections.get(0).links().get(1).targetTitle());
//    assertEquals(-1, sections.get(0).links().get(1).targetId());
    assertEquals(44, sections.get(0).links().get(1).textOffset());

    assertEquals("", sections.get(2).links().get(0).targetTitle());
//    assertEquals(-1, sections.get(2).links().get(0).targetId());
    assertEquals(0, sections.get(2).links().get(0).textOffset());
  }

  @Test
  public void xMLWithoutLinks() {
    page = parser.parseXML(RESOURCES_ROOT.resolve("XMLWithoutLinks").toFile());
//    assertEquals(page.id(), 6675547);
    assertEquals(page.title(), "Бонифаций дель Васто");

    List<Section> sections = page.sections().collect(Collectors.toList());
    assertEquals(sections.size(), 3);
    assertEquals(
        sections.get(2).text().toString(),
        "C.W. Previté-Orton, The Early History of the House of Savoy (1000—1233) (Cambridge, 1912), Malaterra, De rebus gestis Rogerii Calabriæ et Siciliæ comitis et Roberti Guiscardi ducis fratris eius, ed. E. Pontiari, Rerum Italicarum Scriptores, nuova ed. v. 5.1 (Bolgna, 1927—1928). R. Bordone, ‘Affermazione personale e sviluppi dinastici del gruppo parentalae aleramico: il marchese Bonifacio ‘del Vasto’,’ in Formazione e strutture dei ceti dominanti nel medioevo (Atti del I convegno di Pisa: 10-11 maggio 1983) (Rome, 1988), pp. 29-44. L. Provero, Dai marchesi del Vasto ai primi marchesi di Saluzzo. Sviluppi signorili entro quadri pubblici (secoli XI—XII) (Turin, 1992). Giuseppe Sorge, Mussomeli dall’origine all’abolizione della feudalità, vol. II, Catania 1916, poi Edizioni Ristampe Siciliane, Palermo 1982. http://fmg.ac/Projects/MedLands/MONFERRATO,%20SALUZZO,%20SAVONA.htm#_Toc359999490");
  }

  @Test
  public void testUris() {
    page = parser.parseXML(RESOURCES_ROOT.resolve("uriXML").toFile());
    Set<URI> sectionUris = page.sections().map(Section::uri).collect(Collectors.toSet());

    assertEquals(10, sectionUris.size());
    assertTrue(sectionUris.contains(page.uri()));
  }

  @Test
  public void testSections() {
    page = parser.parseXML(RESOURCES_ROOT.resolve("uriXML").toFile());
    List<Section> sections = page.sections().collect(Collectors.toList());

    assertEquals(10, sections.size());
    assertEquals("Ссылки", getTitle(sections, 0));
    assertEquals("Ссылки|Ссылка1", getTitle(sections, 1));
    assertEquals("Ссылки|Ссылка3", getTitle(sections, 2));
    assertEquals("Ссылки|Ссылка2", getTitle(sections, 3));
    assertEquals("Ссылки|Ссылка2|Ссылка1", getTitle(sections, 4));
    assertEquals("Ссылки|Ссылка2|Ссылка2", getTitle(sections, 5));
    assertEquals("Ссылки|Ссылка3", getTitle(sections, 6));
    assertEquals("Ссылки|Ссылка3|Ссылка4", getTitle(sections, 7));
    assertEquals("Ссылки|Ссылка3|Ссылка5", getTitle(sections, 8));
    assertEquals("Не Ссылки", getTitle(sections, 9));
  }

  private String getTitle(List<Section> sections, int i) {
    List<CharSequence> title = sections.get(i).titles();
    return String.join("|", title);
  }
}
