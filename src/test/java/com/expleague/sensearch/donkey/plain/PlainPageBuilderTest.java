package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.utils.SensearchTestCase;
import com.google.protobuf.InvalidProtocolBufferException;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.DBIterator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PlainPageBuilderTest extends SensearchTestCase {

  private static final String PLAIN_DB_ROOT = "PlainDB";
  private static List<CrawlerDocumentMock> crawlerPages = new LinkedList<>();

  @BeforeClass
  public static void initCrawlerPages() {
    crawlerPages.add(new CrawlerDocumentMock("Title1", "Content1"));
    crawlerPages.add(new CrawlerDocumentMock("Title2", "Content2"));
    crawlerPages.add(new CrawlerDocumentMock("Title3", "Content3"));
  }

  @Test
  public void testBuildPlainDb() throws IOException {
    clearOutputRoot();
    Path plainDbPath = testOutputRoot().resolve(PLAIN_DB_ROOT);
    DB plainDb = JniDBFactory.factory.open(plainDbPath.toFile(), dbCreateOptions());

    TLongList pageIds = new TLongArrayList();
    PlainPageBuilder plainPageBuilder = new PlainPageBuilder(plainDb);
    for (CrawlerDocument cd : crawlerPages) {
      long pageId = plainPageBuilder.add(cd);
      // Test page ids are unique
      Assert.assertFalse(pageIds.contains(pageId));
      pageIds.add(pageId);
    }

    plainPageBuilder.build();
  }

  @Test(expected = DBException.class)
  public void buildPlainDbTwice() throws IOException {
    DB plainDb =
        JniDBFactory.factory.open(
            Files.createTempDirectory(testOutputRoot(), "tmp").toFile(), dbCreateOptions());

    PlainPageBuilder plainPageBuilder = new PlainPageBuilder(plainDb);
    plainPageBuilder.build();

    plainPageBuilder.add(new CrawlerDocumentMock("testTitle", "testContent"));
    plainPageBuilder.build();
  }

  @Test
  public void testReadPlainDb() throws IOException {
    testBuildPlainDb();
    try (DB plainDb =
        JniDBFactory.factory.open(
            testOutputRoot().resolve(PLAIN_DB_ROOT).toFile(), dbOpenOptions())) {

      List<Page> dbContent = new LinkedList<>();
      DBIterator dbIterator = plainDb.iterator();
      dbIterator.seekToFirst();
      dbIterator.forEachRemaining(
          kv -> {
            try {
              dbContent.add(Page.parseFrom(kv.getValue()));
            } catch (InvalidProtocolBufferException e) {
              Assert.fail("Data base contain invalid protobuf!");
            }
          });

      Assert.assertEquals(crawlerPages.size(), dbContent.size());
      for (CrawlerDocument cd : crawlerPages) {

        boolean hasPageInDb = false;
        for (Page p : dbContent) {
          hasPageInDb |= (cd.content().equals(p.getContent()) && cd.title().equals(p.getTitle()));
        }

        if (!hasPageInDb) {
          Assert.fail(
              String.format(
                  "Document with title '%s' and content '%s' was not found in base!",
                  cd.title(), cd.content()));
        }
      }
    }
  }

  static class CrawlerDocumentMock implements CrawlerDocument {

    String title;
    String content;

    public CrawlerDocumentMock(String title, String content) {
      this.title = title;
      this.content = content;
    }

    @Override
    public CharSequence content() {
      return content;
    }

    @Override
    public String title() {
      return title;
    }

    @Override
    public List<String> categories() {
      return null;
    }

    @Override
    public List<Section> sections() {
      return null;
    }

    @Override
    public long iD() {
      return 0;
    }

    @Override
    public URI uri() {
      return null;
    }
  }
}
