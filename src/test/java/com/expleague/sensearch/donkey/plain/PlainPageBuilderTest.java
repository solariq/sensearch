package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.utils.CrawlerBasedTestCase;
import com.google.protobuf.InvalidProtocolBufferException;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.DBIterator;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class PlainPageBuilderTest extends CrawlerBasedTestCase {

  private static final String PLAIN_DB_ROOT = "PlainDB";

  @Test
  public void testBuildPlainDb() throws IOException, XMLStreamException {
    clearOutputRoot();
    Path plainDbPath = testOutputRoot().resolve(PLAIN_DB_ROOT);
    DB plainDb = JniDBFactory.factory.open(plainDbPath.toFile(), dbCreateOptions());

    PlainPageBuilder plainPageBuilder = new PlainPageBuilder(plainDb, plainDbPath.resolve("TMP"));
    long[] flushedPageId = new long[]{0};
    crawler().makeStream().forEach(
        cd -> {
          plainPageBuilder.startPage(cd.id(), flushedPageId[0], Collections.emptyList());
          cd.sections().forEach(
              s -> {
                plainPageBuilder.addSection(flushedPageId[0], s);
                ++flushedPageId[0];
              }
          );
        }
    );
    plainPageBuilder.build();
  }

  @Test(expected = DBException.class)
  @Ignore
  public void buildPlainDbTwice() throws IOException, XMLStreamException {
    DB plainDb =
        JniDBFactory.factory.open(
            Files.createTempDirectory(testOutputRoot(), "tmp").toFile(), dbCreateOptions());

    PlainPageBuilder plainPageBuilder = new PlainPageBuilder(plainDb, testOutputRoot()
        .resolve("TMP_PLAIN")
    );
    plainPageBuilder.build();
    plainPageBuilder.build();
  }

  @Test
  public void testReadPlainDb() throws IOException, XMLStreamException {
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

//      Assert.assertEquals(crawlerPages.size(), dbContent.size());
//      for (CrawlerDocument cd : crawlerPages) {
//
//        boolean hasPageInDb = false;
//        for (Page p : dbContent) {
//          hasPageInDb |= (cd.content().equals(p.getContent()) && cd.title().equals(p.getTitle()));
//        }
//
//        if (!hasPageInDb) {
//          Assert.fail(
//              String.format(
//                  "Document with title '%s' and content '%s' was not found in base!",
//                  cd.title(), cd.content()));
//        }
//      }
    }
  }
}
