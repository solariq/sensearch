package com.expleague.sensearch.donkey.crawler;

import com.expleague.sensearch.ConfigJson;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class UnitCrawlerTest {

  private Crawler crawler;
  private Path pathToTmpDoc = Paths.get("TMPDOCS");

  @Test
  public void goodZIPTest() throws IOException, XMLStreamException {
    Config config = new Config(
        Paths.get("./src/test/java/com/expleague/sensearch/donkey/crawler/resources/MiniWiki.zip"),
        pathToTmpDoc
    );
    crawler = new CrawlerXML(config);

    Assert.assertEquals(crawler.makeStream().count(), 20);
  }

  @Test
  public void badZIPTest() throws IOException, XMLStreamException {
    Config config = new Config(
        Paths.get(
            "./src/test/java/com/expleague/sensearch/donkey/crawler/resources/Mini_Wiki_broken.zip"),
        pathToTmpDoc
    );
    crawler = new CrawlerXML(config);

    Assert.assertEquals(crawler.makeStream().count(), 8);

    Set<String> titles = new HashSet<>();
    Set<String> rightTitles = new HashSet<>();
    rightTitles.add("Пантусов, Николай Николаевич");
    rightTitles.add("Нагуманов, Андрей Рафаилович");
    rightTitles.add("Орисаба");
    rightTitles.add("Масатепек");
    rightTitles.add("Тельчак-Пуэбло");
    rightTitles.add("Сантьяго-Куаутлальпан");
    rightTitles.add("Нытва");
    crawler.makeStream().forEach(doc -> {
      if (doc != null) {
        titles.add(doc.title());
      }
    });
    Assert.assertEquals(titles, rightTitles);
  }


  @After
  public void clear() throws IOException {
    FileUtils.deleteDirectory(
        Paths.get("./src/test/java/com/expleague/sensearch/donkey/crawler/resources/TMPDOCS")
            .toFile());
  }

  private class Config implements ConfigJson {

    Path zip;
    Path doc;

    public Config(Path pathToZip, Path tmpDocs) {
      this.zip = pathToZip;
      this.doc = tmpDocs;
    }

    @Override
    public Path getTemporaryDocuments() {
      return doc;
    }

    @Override
    public String getTemporaryBigrams() {
      return null;
    }

    @Override
    public Path getBigramsFileName() {
      return null;
    }

    @Override
    public Path getTemporaryIndex() {
      return null;
    }

    @Override
    public String getWebRoot() {
      return null;
    }

    @Override
    public Path getMyStem() {
      return null;
    }

    @Override
    public Path getPathToZIP() {
      return zip;
    }

    @Override
    public String getStatisticsFileName() {
      return null;
    }

    @Override
    public String getEmbeddingVectors() {
      return null;
    }

    @Override
    public Path getPathToMetrics() {
      return null;
    }
  }
}
