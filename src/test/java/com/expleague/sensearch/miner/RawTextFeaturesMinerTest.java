package com.expleague.sensearch.miner;

import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.miner.impl.RawTextFeaturesMiner;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by sandulmv on 02.11.18.
 */
public class RawTextFeaturesMinerTest {

  private static final Pattern SIMPLE_SPLITTER = Pattern.compile("[^а-яё0-9]+");
  private static final Map<String, String> DOCUMENTS_AND_TITLES = new HashMap<>();
  private static FeaturesMiner miner;
  private static List<Page> pages;

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

  @BeforeClass
  public static void init() {
    miner = new RawTextFeaturesMiner(new IndexStub());
    pages = new ArrayList<>();
    for (Map.Entry<String, String> entry : DOCUMENTS_AND_TITLES.entrySet()) {
      pages.add(new PageStub(entry.getKey(), entry.getValue()));
    }
  }

  @Test
  public void singleWordsQueriesTest() {
    Query qeury1 = new QueryStub("твои");
    pages.forEach(
        p -> {
          Features features = miner.extractFeatures(qeury1, p);
          System.out.printf("BM25: %f\n", features.bm25());
          System.out.printf("Fuzzy: %f\n", features.fuzzy());
        }
    );

  }

  @Test
  public void multipleWordsQueriesTest() {

  }

  private static class TermStub implements Term {

    private final String raw;

    TermStub(String raw) {
      this.raw = raw;
    }

    @Override
    public CharSequence getRaw() {
      return raw;
    }

    @Override
    public CharSequence getNormalized() {
      throw new UnsupportedOperationException();
    }

    @Override
    public LemmaInfo getLemma() {
      throw new UnsupportedOperationException();
    }
  }

  private static class QueryStub implements Query {

    private final List<Term> terms = new ArrayList<>();

    QueryStub(String query) {
      for (String raw : SIMPLE_SPLITTER.split(query.toLowerCase())) {
        terms.add(new TermStub(raw));
      }
    }

    @Override
    public List<Term> getTerms() {
      return terms;
    }

  }

  private static class PageStub implements IndexedPage {

    private final String title;
    private final String content;

    PageStub(String title, String content) {
      this.title = title;
      this.content = content;
    }

    @Override
    public URI reference() {
      throw new UnsupportedOperationException();
    }

    @Override
    public CharSequence title() {
      return title;
    }

    @Override
    public CharSequence text() {
      return content;
    }

    @Override
    public long id() {
      return 0;
    }
  }

  private static class IndexStub implements Index {

    private final List<IndexedPage> availablePages = new ArrayList<>();
    private final TObjectIntMap<String> pagesWithTermCounts = new TObjectIntHashMap<>();
    private final TObjectIntMap<String> termsRawCounts = new TObjectIntHashMap<>();

    private final double averageWordsPerPage;

    IndexStub() {
      int pagesSummaryLength = 0;

      for (Map.Entry<String, String> entry : DOCUMENTS_AND_TITLES.entrySet()) {
        availablePages.add(new PageStub(entry.getKey(), entry.getValue()));

        String[] tokens = SIMPLE_SPLITTER
            .split((entry.getKey() + " " + entry.getValue()).toLowerCase());
        pagesSummaryLength += tokens.length;
        TObjectIntMap<String> uniqueTermsCounts = new TObjectIntHashMap<>();
        for (String token : tokens) {
          if (!uniqueTermsCounts.containsKey(token)) {
            uniqueTermsCounts.put(token, 0);
          }
          uniqueTermsCounts.increment(token);
        }
        uniqueTermsCounts.forEachEntry(
            (s, v) -> {
              termsRawCounts.adjustOrPutValue(s, v, v);
              pagesWithTermCounts.adjustOrPutValue(s, 1, 1);
              return true;
            }
        );
      }

      averageWordsPerPage = 1. * pagesSummaryLength / availablePages.size();
    }

    @Override
    public int indexSize() {
      return availablePages.size();
    }

    @Override
    public int vocabularySize() {
      return termsRawCounts.size();
    }

    @Override
    public double averageWordsPerPage() {
      return averageWordsPerPage;
    }

    @Override
    public int pagesWithTerm(Term term) {
      return pagesWithTermCounts.get(term.getRaw());
    }

    @Override
    public Stream<Page> fetchDocuments(Query query) {
      return availablePages.stream().map(Page.class::cast);
    }

    @Override
    public long termCollectionFrequency(Term term) {
      return termsRawCounts.get(term.getRaw());
    }
  }
}
