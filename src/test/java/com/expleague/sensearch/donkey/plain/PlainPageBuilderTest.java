package com.expleague.sensearch.donkey.plain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.expleague.sensearch.donkey.crawler.document.WikiPage.WikiLink;
import com.expleague.sensearch.donkey.crawler.document.WikiPage.WikiSection;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.utils.CrawlerBasedTestCase;
import com.google.common.primitives.Longs;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PlainPageBuilderTest extends CrawlerBasedTestCase {

  private static final Path PAGE_DB_PATH = Paths.get("testPageDbPath");
  private static final Path TEMP_FILES_ROOT = Paths.get("tempPagesPath");

  @Before
  public void beforeTest() throws IOException {
    Files.createDirectories(PAGE_DB_PATH);
  }

  @After
  public void afterTest() throws IOException {
    FileUtils.deleteDirectory(PAGE_DB_PATH.toFile());
  }

  @Test
  public void testPageWithSubsections() throws IOException {

    Map<String, Long> idsFromPageBuilder = new HashMap<>();
    long rootPageId;

    try (PlainPageBuilder pageBuilder =
        new PlainPageBuilder(
            JniDBFactory.factory.open(PAGE_DB_PATH.toFile(), new Options().errorIfExists(true)),
            TEMP_FILES_ROOT,
            new IdGenerator())) {
      rootPageId =
          pageBuilder.startPage(
              1, Arrays.asList("Category1", "Category2"), URI.create("http://someuri"));

      idsFromPageBuilder.put(
          "someuri#root",
          pageBuilder.addSection(
              new WikiSection(
                  "Some text for first page",
                  Collections.singletonList("First titles"),
                  Collections.emptyList(),
                  URI.create("someuri#root"))));

      idsFromPageBuilder.put(
          "suburi",
          pageBuilder.addSection(
              new WikiSection(
                  "First page subsection",
                  Arrays.asList("First titles", "Subtitle"),
                  Collections.emptyList(),
                  URI.create("suburi"))));

      idsFromPageBuilder.put(
          "suburi2",
          pageBuilder.addSection(
              new WikiSection(
                  "Another first page subsection",
                  Arrays.asList("First titles", "Subtitle2"),
                  Collections.emptyList(),
                  URI.create("suburi2"))));

      idsFromPageBuilder.put(
          "deeper",
          pageBuilder.addSection(
              new WikiSection(
                  "Deeper",
                  Arrays.asList("First titles", "Subtitle2", "Going deeper"),
                  Collections.emptyList(),
                  URI.create("deeper"))));

      idsFromPageBuilder.put(
          "evenmore",
          pageBuilder.addSection(
              new WikiSection(
                  "text text text",
                  Arrays.asList("First titles", "Subtitle2", "Going deeper", "Even more"),
                  Collections.emptyList(),
                  URI.create("evenmore"))));

      idsFromPageBuilder.put(
          "almosttop",
          pageBuilder.addSection(
              new WikiSection(
                  "Almost on the top",
                  Arrays.asList("First titles", "Title"),
                  Collections.emptyList(),
                  URI.create("almosttop"))));

      pageBuilder.endPage();
    }

    Map<Long, Page> pages = new HashMap<>();
    Map<String, Page> pageByUri = new HashMap<>();
    readPagesFromDb(pages, pageByUri);

    System.out.println(idsFromPageBuilder.keySet());
    System.out.println(pageByUri.keySet());

    // Check ids that return builder's methods
    idsFromPageBuilder.forEach(
        (uri, id) -> assertEquals(id.longValue(), pageByUri.get(uri).getPageId()));
    assertEquals(rootPageId, pageByUri.get("someuri#root").getPageId());

    assertEquals(6, pages.size());
    // Only root page doesn't have parent
    assertEquals(5, pages.values().stream().filter(Page::hasParentId).count());
    assertFalse(pageByUri.get("someuri#root").hasParentId());

    // All sections have the same categories
    pages
        .values()
        .forEach(
            page ->
                assertEquals(Arrays.asList("Category1", "Category2"), page.getCategoriesList()));

    // No links are set
    assertEquals(
        0, pages.values().stream().filter(page -> page.getIncomingLinksCount() > 0).count());
    assertEquals(
        0, pages.values().stream().filter(page -> page.getOutgoingLinksCount() > 0).count());

    // Check titles
    assertEquals("First titles", pageByUri.get("someuri#root").getTitle());
    assertEquals("Subtitle", pageByUri.get("suburi").getTitle());
    assertEquals("Subtitle2", pageByUri.get("suburi2").getTitle());
    assertEquals("Going deeper", pageByUri.get("deeper").getTitle());
    assertEquals("Even more", pageByUri.get("evenmore").getTitle());
    assertEquals("Title", pageByUri.get("almosttop").getTitle());

    // Check content for some pages
    assertEquals("Almost on the top", pageByUri.get("almosttop").getContent());
    assertEquals("Some text for first page", pageByUri.get("someuri#root").getContent());

    // Check parents
    assertEquals(pageByUri.get("someuri#root"), pages.get(pageByUri.get("suburi").getParentId()));
    assertEquals(pageByUri.get("someuri#root"), pages.get(pageByUri.get("suburi2").getParentId()));
    assertEquals(pageByUri.get("suburi2"), pages.get(pageByUri.get("deeper").getParentId()));
    assertEquals(pageByUri.get("deeper"), pages.get(pageByUri.get("evenmore").getParentId()));
    assertEquals(
        pageByUri.get("someuri#root"), pages.get(pageByUri.get("almosttop").getParentId()));
  }

  @Test
  public void testMultiplePages() throws IOException {
    long page1Id, page2Id, page3Id;

    try (PlainPageBuilder pageBuilder =
        new PlainPageBuilder(
            JniDBFactory.factory.open(PAGE_DB_PATH.toFile(), new Options().errorIfExists(true)),
            TEMP_FILES_ROOT,
            new IdGenerator())) {
      page1Id =
          pageBuilder.startPage(1, Arrays.asList("Category 1", "Category 2"), URI.create("Page1"));
      pageBuilder.addSection(
          new WikiSection(
              "Some text",
              Collections.singletonList("Some titles"),
              Collections.emptyList(),
              URI.create("Page1#root")));
      pageBuilder.endPage();

      // Page without sections should not be inserted
      pageBuilder.startPage(239, Arrays.asList("1", "2"), URI.create("Page239"));
      pageBuilder.endPage();

      page2Id = pageBuilder.startPage(2, Arrays.asList("Category 1", "2"), URI.create("Page2"));
      pageBuilder.addSection(
          new WikiSection(
              "second text",
              Collections.singletonList("Second titles"),
              Collections.emptyList(),
              URI.create("Page2#root")));
      pageBuilder.addSection(
          new WikiSection(
              "second text in subsection",
              Arrays.asList("Second titles", "Second subtitle"),
              Collections.emptyList(),
              URI.create("Page2subsection")));
      pageBuilder.endPage();

      page3Id = pageBuilder.startPage(3, Arrays.asList("Category 1", "222"), URI.create("Page3"));
      pageBuilder.addSection(
          new WikiSection(
              "third text",
              Collections.singletonList("Third titles"),
              Collections.emptyList(),
              URI.create("Page3#root")));
      pageBuilder.endPage();
    }

    Map<Long, Page> pages = new HashMap<>();
    Map<String, Page> pageByUri = new HashMap<>();
    readPagesFromDb(pages, pageByUri);

    assertEquals(4, pages.size());

    assertEquals(page1Id, pageByUri.get("Page1#root").getPageId());
    assertEquals(page2Id, pageByUri.get("Page2#root").getPageId());
    assertEquals(page3Id, pageByUri.get("Page3#root").getPageId());

    // Page without sections should not be presented in the database
    assertFalse(pageByUri.containsKey("Page239"));

    assertTrue(pageByUri.containsKey("Page1#root"));
    assertTrue(pageByUri.containsKey("Page2#root"));
    assertTrue(pageByUri.containsKey("Page3#root"));

    // Check parents
    assertEquals(
        pageByUri.get("Page2#root"), pages.get(pageByUri.get("Page2subsection").getParentId()));
    assertFalse(pageByUri.get("Page1#root").hasParentId());
    assertFalse(pageByUri.get("Page2#root").hasParentId());
    assertFalse(pageByUri.get("Page3#root").hasParentId());
  }

  @Test
  public void testLinks() throws IOException {
    try (PlainPageBuilder pageBuilder =
        new PlainPageBuilder(
            JniDBFactory.factory.open(PAGE_DB_PATH.toFile(), new Options().errorIfExists(true)),
            TEMP_FILES_ROOT,
            new IdGenerator())) {
      pageBuilder.startPage(1, Arrays.asList("Category1", "Category2"), URI.create("Page1"));

      pageBuilder.addSection(
          new WikiSection(
              "Some text for first page",
              Collections.singletonList("First titles"),
              Collections.singletonList(new WikiLink("text", "First titles", 1, 5)),
              URI.create("Page1#root")));

      pageBuilder.addSection(
          new WikiSection(
              "First page subsection",
              Arrays.asList("First titles", "Subtitle"),
              Collections.singletonList(new WikiLink("page", "Third titles", 3, 6)),
              URI.create("suburi")));

      pageBuilder.addSection(
          new WikiSection(
              "Another first page subsection",
              Arrays.asList("First titles", "Subtitle2"),
              Arrays.asList(
                  new WikiLink("Another", "Second titles", 2, 0),
                  new WikiLink("subsection", "First titles", 1, 19)),
              URI.create("suburi2")));

      pageBuilder.endPage();

      pageBuilder.startPage(2, Collections.singletonList("Category 1"), URI.create("Page2"));

      pageBuilder.addSection(
          new WikiSection(
              "Second page section text",
              Collections.singletonList("Second page"),
              Collections.emptyList(),
              URI.create("Page2#root")));

      pageBuilder.addSection(
          new WikiSection(
              "Second page subsection text",
              Arrays.asList("Second page", "Second page subsection"),
              Arrays.asList(
                  new WikiLink("Second", "Second page", 2, 0),
                  new WikiLink("page", "First page", 1, 6)),
              URI.create("Page2subpage")));

      pageBuilder.endPage();

      pageBuilder.startPage(3, Collections.emptyList(), URI.create("Page3"));

      pageBuilder.addSection(
          new WikiSection(
              "Third page section text",
              Collections.singletonList("Third page"),
              Arrays.asList(
                  new WikiLink("Third", "Third page", 3, 0),
                  new WikiLink("page", "Unexisting page", -1, 7)),
              URI.create("Page3#root")));
      pageBuilder.endPage();
    }

    Map<Long, Page> pages = new HashMap<>();
    Map<String, Page> pageByUri = new HashMap<>();
    readPagesFromDb(pages, pageByUri);

    assertEquals(3, pageByUri.get("Page1#root").getIncomingLinksCount());
    // NOTE: these links are only links from the specific SECTION
    assertEquals(1, pageByUri.get("Page1#root").getOutgoingLinksCount());
    // As for incoming links, they always lead to the root section, so non-root sections always have
    // 0 incoming links
    assertEquals(0, pageByUri.get("suburi").getIncomingLinksCount());
    assertEquals(1, pageByUri.get("suburi").getOutgoingLinksCount());
    assertEquals(0, pageByUri.get("suburi2").getIncomingLinksCount());
    assertEquals(2, pageByUri.get("suburi2").getOutgoingLinksCount());

    assertEquals(2, pageByUri.get("Page2#root").getIncomingLinksCount());
    assertEquals(0, pageByUri.get("Page2#root").getOutgoingLinksCount());
    assertEquals(0, pageByUri.get("Page2subpage").getIncomingLinksCount());
    assertEquals(2, pageByUri.get("Page2subpage").getOutgoingLinksCount());

    assertEquals(2, pageByUri.get("Page3#root").getIncomingLinksCount());
    assertEquals(2, pageByUri.get("Page3#root").getOutgoingLinksCount());

    // TODO (tehnar): check link contents
  }

  private void readPagesFromDb(Map<Long, Page> pages, Map<String, Page> pageByUri)
      throws IOException {
    try (DB pageDb = JniDBFactory.factory.open(PAGE_DB_PATH.toFile(), new Options())) {
      DBIterator iterator = pageDb.iterator();
      iterator.seekToFirst();
      iterator.forEachRemaining(
          entry -> {
            try {
              Page page = Page.parseFrom(entry.getValue());
              pages.put(Longs.fromByteArray(entry.getKey()), page);
              pageByUri.put(page.getUri(), page);
            } catch (InvalidProtocolBufferException e) {
              throw new RuntimeException(e);
            }
          });
    }
  }
}
