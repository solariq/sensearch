package com.expleague.sensearch.miner.FeatureTest;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.features.sets.ranker.BM25FeatureSet;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.features.sets.ranker.TextFeatureSet;
import com.expleague.sensearch.features.sets.ranker.TextFeatureSet.Segment;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.utils.IndexBasedTestCase;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;

public class BM25Test extends IndexBasedTestCase {

  private static final double K = 1.2;
  private static final double B = 0.75;

  private TextFeatureSet testBM25;

  private static final String TITLE_1 = "Суффет";
  private static final String PAGE_1 =
      "Суффет — название двух главных должностных лиц (магистратов) в Тире, а также в северной Африке, на территории Карфагенской республики."
      + " Обычно они были верховными судьями. Во время военных действий часто — главнокомандующими."
      + " В древнем Израиле так называли военных предводителей и судей.";

  private static final String TITLE_2 = "Правитель";
  private static final String PAGE_2 =
      "Прави́тель, Прави́тельница — глава государства, страны или иной обособленной территории.\n"
          + "\n"
          + "Слово «правитель» не имеет иноязычного происхождения, а потому является приемлемым для обозначения главы государства любого политического устройства, формы правления или культуры."
          + " Также этим словом можно называть регентов и узурпаторов."
          + " По этим же причинам понятие «правитель» является более точным и верным, нежели слово «царь», в обозначении титулов монархов древности."
          + " Правитель — лицо, которое правит государством, страной.";

  private static final double averageTitleLen = (((int) index().parse(TITLE_1).count())
      + ((int) index().parse(TITLE_2).count())) / 2.0;
  private static final double averageContentLen = (((int) index().parse(PAGE_1).count())
      + ((int) index().parse(PAGE_2).count())) / 2.0;
  private static final double averageTotalLen = averageContentLen + averageTitleLen;
  private static final int TMP_INDEX_SIZE = 2;

  private void init(String title, String page) {
    int titLen = (int) index().parse(title).count();
    int pagLen = (int) index().parse(page).count();
    int totLen = titLen + pagLen;

    testBM25.withStats(totLen,
        averageTotalLen,
        titLen,
        averageTitleLen,
        pagLen,
        averageContentLen,
        TMP_INDEX_SIZE);
    index().parse(title).forEach(t -> {
      testBM25.withSegment(Segment.TITLE, t);
      testBM25.withTerm(t, 0);
    });
    index().parse(page).forEach(t -> {
      testBM25.withSegment(Segment.BODY, t);
      testBM25.withTerm(t, 0);
    });
  }

  @Test
  public void testPage1() {
    testBM25 = new BM25FeatureSet();
    Query query = BaseQuery.create("Суффет", index());
    QURLItem item = new QURLItem(new TestPage(), query);
    testBM25.accept(item);
    init(TITLE_1, PAGE_1);
    int titLen = (int) index().parse(TITLE_1).count();
    int pagLen = (int) index().parse(PAGE_1).count();
    int totLen = titLen + pagLen;

    Vec resBM25 = testBM25.advance();
    double scoreBM25 = Math.log((double) TMP_INDEX_SIZE / query.terms().get(0).documentFreq()) * 2 / (2 + K * (1 - B + B * totLen / averageTotalLen));
    double scoreBM25L = Math.log((double) TMP_INDEX_SIZE / query.terms().get(0).documentLemmaFreq()) * 2 / (2 + K * (1 - B + B * totLen / averageTotalLen));
//    System.err.println("Query: Суффет   Vec: " + resBM25);
    Assert.assertEquals(0, Double.compare(resBM25.get(0), scoreBM25));
    Assert.assertEquals(0, Double.compare(resBM25.get(1), scoreBM25L));


    query = BaseQuery.create("Военные предводители", index());
    item = new QURLItem(new TestPage(), query);
    testBM25.accept(item);
    init(TITLE_1, PAGE_1);

    resBM25 = testBM25.advance();
    scoreBM25 = 0 + 0;
    scoreBM25L = Math.log((double) TMP_INDEX_SIZE / query.terms().get(0).documentLemmaFreq()) * 2 / (2 + K * (1 - B + B * totLen / averageTotalLen))
        + Math.log((double) TMP_INDEX_SIZE / query.terms().get(1).documentLemmaFreq()) * 1 / (1 + K * (1 - B + B * totLen / averageTotalLen));

//    System.err.println("Query: Военные предводители   Vec: " + resBM25);
//    System.err.println(query.terms().get(0).lemma().documentLemmaFreq());
//    System.err.println(query.terms().get(1).lemma().documentLemmaFreq());
//    System.err.println();
//    index().parse(PAGE_1).forEach(t -> System.err.println(t.lemma().text()));

    Assert.assertEquals(0, Double.compare(resBM25.get(0), scoreBM25));
    Assert.assertEquals(0, Double.compare(resBM25.get(1), scoreBM25L));

  }

  @Test
  public void testPage2() {
    testBM25 = new BM25FeatureSet();
    Query query = BaseQuery.create("Правитель", index());
    QURLItem item = new QURLItem(new TestPage(), query);
//    query.terms().forEach(t -> System.err.println(t.lemma().text()));
//    System.err.println("====================");
//    index().parse(PAGE_2).forEach(t -> System.err.println(t.lemma().text()));
    testBM25.accept(item);
    init(TITLE_2, PAGE_2);

    int titLen = (int) index().parse(TITLE_2).count();
    int pagLen = (int) index().parse(PAGE_2).count();
    int totLen = titLen + pagLen;

    Vec resBM25 = testBM25.advance();
    double scoreBM25 = Math.log((double) TMP_INDEX_SIZE / query.terms().get(0).documentFreq()) * 4 / (4 + K * (1 - B + B * totLen / averageTotalLen));
    double scoreBM25L = Math.log((double) TMP_INDEX_SIZE / query.terms().get(0).documentLemmaFreq()) * 4 / (4 + K * (1 - B + B * totLen / averageTotalLen));
//    System.err.println("Query: Правитель   Vec: " + resBM25);
//    System.err.println("resBM25: " + scoreBM25 + " resBM25L: " + scoreBM25L);
    Assert.assertEquals(0, Double.compare(resBM25.get(0), scoreBM25));
    Assert.assertEquals(0, Double.compare(resBM25.get(1), scoreBM25L));

  }

  private class TestPage implements Page {

    @Override
    public URI uri() {
      return URI.create("uri");
    }

    @Override
    public CharSequence content(SegmentType... types) {
      return null;
    }

    @Override
    public List<CharSequence> categories() {
      return null;
    }

    @Override
    public Stream<Link> outgoingLinks(LinkType type) {
      return null;
    }

    @Override
    public Stream<Link> incomingLinks(LinkType type) {
      return null;
    }

    @Override
    public Page parent() {
      return null;
    }

    @Override
    public Page root() {
      return this;
    }

    @Override
    public boolean isRoot() {
      return true;
    }

    @Override
    public Stream<Page> subpages() {
      return null;
    }

    @Override
    public Stream<CharSequence> sentences(SegmentType type) {
      return null;
    }
  }

}
