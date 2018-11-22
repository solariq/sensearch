package com.expleague.sensearch.index;

import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.index.plain.PlainIndexBuilder;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by sandulmv on 19.10.18.
 */
public class MinimalFunctionalityPlainIndexTest {

  private static final String CONTENT_FILE = "content";
  private static final String META_FILE = "meta";

  private static final Logger LOG =
      Logger.getLogger(MinimalFunctionalityPlainIndexTest.class.getName());

  private static final Map<String, String> DOCUMENTS_AND_TITLES = new HashMap<>();

  // init documents for crawler
  static {

    DOCUMENTS_AND_TITLES.put(
        "Радиожираф",
        "-- Как живете караси?"
            + "-- Ничего себе, мерси..."
    );

    DOCUMENTS_AND_TITLES.put(
        "Откровение Иоанна Богослова",
        "Знаю дела твои, и труд твой, и терпение твое, и то, что ты не можешь сносить "
            + "развратных, и испытал тех, которые называют себя Апостолами, а они не таковы, и "
            + "нашел, что они лжецы..."
    );

    DOCUMENTS_AND_TITLES.put(
        "Часть первая. Мусорщик",
        "Баки были ржавые, помятые, с отставшими крышками. "
            + "Из-под крышек торчали обрывки газет, свешивалась картофельная шелуха. "
            + "Это было похоже на пасть неопрятного, неразборчивого в еде пеликана..."
    );
  }

  private Index plainIndex;
  private Path indexRoot;

  @Before
  public void initIndex() throws Exception {
    indexRoot = Files.createTempDirectory(Paths.get(System.getProperty("user.dir")), "tmp");
    LOG.info(String.format("Will use the following path as index root: %s",
        indexRoot.toAbsolutePath().toString()));

    Crawler localCrawler = new LocalCrawler();
    plainIndex = new PlainIndexBuilder(new Config()).buildIndex(localCrawler.makeStream());
  }

  @Test
  public void indexStructureTest() throws Exception {
    int documentsCount = DOCUMENTS_AND_TITLES.size();
    Assert.assertTrue(Files.exists(indexRoot));

    List<Path> indexEntries = Files.list(indexRoot).collect(Collectors.toList());
    Assert.assertEquals(indexEntries.size(), documentsCount);

    for (Path indexEntry : indexEntries) {
      Assert.assertTrue(Files.exists(indexEntry.resolve(CONTENT_FILE)));
      Assert.assertTrue(Files.exists(indexEntry.resolve(META_FILE)));
    }
  }

  @Test
  public void indexFunctionalityTest() {
    plainIndex.fetchDocuments(new QueryStub("empty")).forEach(
        doc -> {
          Assert.assertTrue(DOCUMENTS_AND_TITLES.containsKey(doc.title().toString()));
          Assert.assertEquals(
              DOCUMENTS_AND_TITLES.get(doc.title().toString()),
              doc.text().toString()
          );
        }
    );
  }

  @After
  public void cleanup() throws Exception {
    FileUtils.deleteDirectory(indexRoot.toFile());
  }

  private static class QueryStub implements Query {

    private List<Term> terms;

    QueryStub(String query) {
      terms = new LinkedList<>();
      for (String w : query.split("[^А-ЯЁа-яёA-Za-z0-9]")) {
        terms.add(new TermStub(w));
      }
    }

    @Override
    public List<Term> getTerms() {
      return terms;
    }
  }

  private static class TermStub implements Term {

    private String rawTerm;

    TermStub(String term) {
      rawTerm = term;
    }

    @Override
    public CharSequence getRaw() {
      return rawTerm;
    }

    @Override
    public CharSequence getNormalized() {
      return null;
    }

    @Override
    public LemmaInfo getLemma() {
      return null;
    }
  }

  private static class TextDocument implements CrawlerDocument {

    private final String title;
    private final String content;

    TextDocument(String title, String content) {
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
      return new ArrayList<>();
    }

    @Override
    public List<Section> sections() {
      return Collections.singletonList(new Section() {
        @Override
        public CharSequence text() {
          return content;
        }

        @Override
        public CharSequence title() {
          return "Some section title";
        }
      });
    }

    @Override
    public long iD() {
      throw new UnsupportedOperationException();
    }
  }

  private static class LocalCrawler implements Crawler {

    private List<CrawlerDocument> crawledDocuments;

    LocalCrawler() {
      crawledDocuments = new LinkedList<>();
      for (Map.Entry<String, String> docAndTitle : DOCUMENTS_AND_TITLES.entrySet()) {
        crawledDocuments.add(new TextDocument(docAndTitle.getKey(), docAndTitle.getValue()));
      }
    }

    @Override
    public Stream<CrawlerDocument> makeStream() {
      return crawledDocuments.stream();
    }
  }

}
