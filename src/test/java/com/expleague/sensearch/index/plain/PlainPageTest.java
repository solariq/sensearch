package com.expleague.sensearch.index.plain;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.Link;
import com.expleague.sensearch.Page.LinkType;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.utils.IndexBasedTestCase;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class PlainPageTest extends IndexBasedTestCase {

  private static final URI TEST_PAGE_URI =
      URI.create("https://ru.wikipedia.org/wiki/Тестовый_заголовок");
  private static final URI SECTION_URI_1 =
      URI.create("https://ru.wikipedia.org/wiki/Тестовый_заголовок#Тестовый_заголовок");
  private static final URI SECTION_URI_2 =
      URI.create("https://ru.wikipedia.org/wiki/Тестовый_заголовок#Подсекция");
  private static final URI SECTION_URI_3 =
      URI.create("https://ru.wikipedia.org/wiki/Тестовый_заголовок#И_еще_одна");
  private static final URI SECTION_URI_4 =
      URI.create("https://ru.wikipedia.org/wiki/Тестовый_заголовок#Биография");

  private final Page rootPage = index().page(TEST_PAGE_URI);
  private final Page page1 = index().page(SECTION_URI_1);
  private final Page page2 = index().page(SECTION_URI_2);
  private final Page page3 = index().page(SECTION_URI_3);
  private final Page page4 = index().page(SECTION_URI_4);

  @Test
  public void testExists() {
    assertNotSame(PlainPage.EMPTY_PAGE, rootPage);
    assertNotSame(PlainPage.EMPTY_PAGE, page1);
    assertNotSame(PlainPage.EMPTY_PAGE, page2);
    assertNotSame(PlainPage.EMPTY_PAGE, page3);
    assertNotSame(PlainPage.EMPTY_PAGE, page4);
  }

  @Test
  public void testMissingPage() {
    assertSame(PlainPage.EMPTY_PAGE, index().page(URI.create("https://invalid_page")));
  }

  @Test
  public void testIsRoot() {
    assertTrue(rootPage.isRoot());
    assertTrue(page1.isRoot());

    assertFalse(page2.isRoot());
    assertFalse(page3.isRoot());
    assertFalse(page4.isRoot());
  }

  @Test
  public void testRoot() {
    assertEquals(rootPage, rootPage.root());
    assertEquals(rootPage, page1.root());
    assertEquals(rootPage, page2.root());
    assertEquals(rootPage, page3.root());
    assertEquals(rootPage, page4.root());
  }

  @Test
  public void testSectionParents() {
    assertEquals(page1, page2.parent());
    assertEquals(page2, page3.parent());
    assertEquals(page1, page4.parent());
  }

  @Test
  public void testSubpages() {
    assertEquals(2, rootPage.subpages().count());
    assertEquals(Arrays.asList(page2, page4), rootPage.subpages().collect(Collectors.toList()));
  }

  @Test
  public void testUri() throws UnsupportedEncodingException {
    assertEquals(SECTION_URI_1.toString(), URLDecoder.decode(rootPage.uri().toString(), "UTF-8"));
  }

  @Test
  public void testContentBody() {
    String content1 = "Параграф номер один.\n" + "Второй параграф с ссылкой .";
    String content2 = "Какой-то текст.";
    String content3 =
        "Текст про то, насколько интересная эта статья.\n"
            + "\n"
            + "В этой секции есть целых два параграфа, круто!";
    String content4 = "А это ссылка на эту же страницу , а это - на другую: про регентов .";

    assertEquals(content1, page1.content(SegmentType.SUB_BODY));
    assertEquals(content2, page2.content(SegmentType.SUB_BODY));
    assertEquals(content3, page3.content(SegmentType.SUB_BODY));
    assertEquals(content4, page4.content(SegmentType.SUB_BODY));

    assertEquals(
        String.join("\n", content1, content2, content3, content4), page1.content(SegmentType.BODY));
    assertEquals(String.join("\n", content2, content3), page2.content(SegmentType.BODY));
    assertEquals(String.join("\n", content3), page3.content(SegmentType.BODY));
    assertEquals(String.join("\n", content4), page4.content(SegmentType.BODY));
  }

  @Test
  public void testContentTitle() {
    String title1 = "Тестовый заголовок";
    String title2 = "Подсекция";
    String title3 = "И еще одна";
    String title4 = "Биография";

    assertEquals(title1, page1.content(SegmentType.SECTION_TITLE));
    assertEquals(title2, page2.content(SegmentType.SECTION_TITLE));
    assertEquals(title3, page3.content(SegmentType.SECTION_TITLE));
    assertEquals(title4, page4.content(SegmentType.SECTION_TITLE));

    assertEquals(title1, page1.content(SegmentType.FULL_TITLE));
    assertEquals(
        String.join(Page.TITLE_DELIMETER, title1, title2), page2.content(SegmentType.FULL_TITLE));
    assertEquals(
        String.join(Page.TITLE_DELIMETER, title1, title2, title3),
        page3.content(SegmentType.FULL_TITLE));
    assertEquals(
        String.join(Page.TITLE_DELIMETER, title1, title4), page4.content(SegmentType.FULL_TITLE));
  }

  @Test
  public void categories() {
    List<CharSequence> categories = Collections.singletonList("Правители Кореи");

    assertEquals(categories, rootPage.categories());
    assertEquals(categories, page1.categories());
    assertEquals(categories, page2.categories());
    assertEquals(categories, page3.categories());
    assertEquals(categories, page4.categories());
  }

  @Test
  public void testIncomingLinks() {
    assertEquals(2, rootPage.incomingLinks(LinkType.SECTION_LINKS).count());
    assertEquals(2, rootPage.incomingLinks(LinkType.ALL_LINKS).count());
    assertEquals(3, rootPage.incomingLinks(LinkType.SECTION_INCLUDING_SELF_LINKS).count());
    assertEquals(3, rootPage.incomingLinks(LinkType.ALL_INCLUDING_SELF_LINKS).count());

    assertEquals(2, page1.incomingLinks(LinkType.SECTION_LINKS).count());
    assertEquals(2, page1.incomingLinks(LinkType.ALL_LINKS).count());
    assertEquals(3, page1.incomingLinks(LinkType.SECTION_INCLUDING_SELF_LINKS).count());
    assertEquals(3, page1.incomingLinks(LinkType.ALL_INCLUDING_SELF_LINKS).count());

    assertEquals(0, page2.incomingLinks(LinkType.SECTION_LINKS).count());
    assertEquals(2, page2.incomingLinks(LinkType.ALL_LINKS).count());
    assertEquals(0, page2.incomingLinks(LinkType.SECTION_INCLUDING_SELF_LINKS).count());
    assertEquals(3, page2.incomingLinks(LinkType.ALL_INCLUDING_SELF_LINKS).count());

    assertEquals(0, page3.incomingLinks(LinkType.SECTION_LINKS).count());
    assertEquals(2, page3.incomingLinks(LinkType.ALL_LINKS).count());
    assertEquals(0, page3.incomingLinks(LinkType.SECTION_INCLUDING_SELF_LINKS).count());
    assertEquals(3, page3.incomingLinks(LinkType.ALL_INCLUDING_SELF_LINKS).count());

    assertEquals(0, page4.incomingLinks(LinkType.SECTION_LINKS).count());
    assertEquals(2, page4.incomingLinks(LinkType.ALL_LINKS).count());
    assertEquals(0, page4.incomingLinks(LinkType.SECTION_INCLUDING_SELF_LINKS).count());
    assertEquals(3, page4.incomingLinks(LinkType.ALL_INCLUDING_SELF_LINKS).count());

    Link link =
        rootPage.incomingLinks(LinkType.SECTION_INCLUDING_SELF_LINKS).findFirst().orElse(null);
    assertNotNull(link);
    assertEquals(page4, link.sourcePage());
    assertEquals(rootPage, link.targetPage());
    assertEquals("А это ссылка на эту же страницу", link.text());
  }

  @Test
  public void testOutgoingLinks() {
    assertEquals(3, rootPage.outgoingLinks(LinkType.ALL_INCLUDING_SELF_LINKS).count());
    assertEquals(2, rootPage.outgoingLinks(LinkType.ALL_LINKS).count());
    assertEquals(1, rootPage.outgoingLinks(LinkType.SECTION_INCLUDING_SELF_LINKS).count());
    assertEquals(1, rootPage.outgoingLinks(LinkType.SECTION_LINKS).count());

    assertEquals(3, page1.outgoingLinks(LinkType.ALL_INCLUDING_SELF_LINKS).count());
    assertEquals(2, page1.outgoingLinks(LinkType.ALL_LINKS).count());
    assertEquals(1, page1.outgoingLinks(LinkType.SECTION_INCLUDING_SELF_LINKS).count());
    assertEquals(1, page1.outgoingLinks(LinkType.SECTION_LINKS).count());

    assertEquals(0, page2.outgoingLinks(LinkType.ALL_INCLUDING_SELF_LINKS).count());
    assertEquals(0, page2.outgoingLinks(LinkType.ALL_LINKS).count());
    assertEquals(0, page2.outgoingLinks(LinkType.SECTION_INCLUDING_SELF_LINKS).count());
    assertEquals(0, page2.outgoingLinks(LinkType.SECTION_LINKS).count());

    assertEquals(0, page3.outgoingLinks(LinkType.ALL_INCLUDING_SELF_LINKS).count());
    assertEquals(0, page3.outgoingLinks(LinkType.ALL_LINKS).count());
    assertEquals(0, page3.outgoingLinks(LinkType.SECTION_INCLUDING_SELF_LINKS).count());
    assertEquals(0, page3.outgoingLinks(LinkType.SECTION_LINKS).count());

    assertEquals(2, page4.outgoingLinks(LinkType.ALL_INCLUDING_SELF_LINKS).count());
    assertEquals(1, page4.outgoingLinks(LinkType.ALL_LINKS).count());
    assertEquals(2, page4.outgoingLinks(LinkType.SECTION_INCLUDING_SELF_LINKS).count());
    assertEquals(1, page4.outgoingLinks(LinkType.SECTION_LINKS).count());

    Link link1 = rootPage.outgoingLinks(LinkType.SECTION_LINKS).findFirst().orElse(null);
    assertNotNull(link1);
    assertEquals(rootPage, link1.sourcePage());
    assertFalse(link1.targetExists());
    assertSame(PlainPage.EMPTY_PAGE, link1.targetPage());
    assertEquals("ссылкой", link1.text());

    Link link2 = page4.outgoingLinks(LinkType.SECTION_LINKS).findFirst().orElse(null);
    assertNotNull(link2);
    assertEquals(page4, link2.sourcePage());
    assertEquals(
        index().page(URI.create("https://ru.wikipedia.org/wiki/Регент")), link2.targetPage());
    assertTrue(link2.targetExists());
    assertEquals("про регентов", link2.text());

    Link link3 =
        page4
            .outgoingLinks(LinkType.SECTION_INCLUDING_SELF_LINKS)
            .filter(l -> !l.targetPage().equals(link2.targetPage()))
            .findFirst()
            .orElse(null);
    assertNotNull(link3);
    assertEquals(page4, link3.sourcePage());
    assertEquals(rootPage, link3.targetPage());
    assertTrue(link3.targetExists());
    assertEquals("А это ссылка на эту же страницу", link3.text());
  }

  @Test
  public void sentences() {
    // TODO
  }
}
