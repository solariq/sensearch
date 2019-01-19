package com.expleague.sensearch.miner.FeatureTest;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.miner.impl.BM25FeatureSet;
import com.expleague.sensearch.miner.impl.QURLItem;
import com.expleague.sensearch.miner.impl.TextFeatureSet;
import com.expleague.sensearch.miner.impl.TextFeatureSet.Segment;
import com.expleague.sensearch.query.BaseQuery;
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

  private final String title1 = "Суффет"; //1
  private final String page1 = //37
      "Суффет — название двух главных должностных лиц (магистратов) в Тире, а также в северной Африке, на территории Карфагенской республики."
      + " Обычно они были верховными судьями. Во время военных действий часто — главнокомандующими."
      + " В древнем Израиле так называли военных предводителей и судей.";

  private final String title2 = "Правитель"; //1
  private final String page2 = //62
      "Прави́тель, Прави́тельница — глава государства, страны или иной обособленной территории.\n"
          + "\n"
          + "Слово «правитель» не имеет иноязычного происхождения, а потому является приемлемым для обозначения главы государства любого политического устройства, формы правления или культуры."
          + " Также этим словом можно называть регентов и узурпаторов."
          + " По этим же причинам понятие «правитель» является более точным и верным, нежели слово «царь», в обозначении титулов монархов древности."
          + " Правитель — лицо, которое правит государством, страной.";

  @Test
  public void testPage1() {
    testBM25 = new BM25FeatureSet();
    QURLItem item = new QURLItem(new TestPage(), BaseQuery.create("Суффет", index()));
    testBM25.accept(item);

    testBM25.withStats(38,
        (38 + 63) / 2.,
        1,
        1.,
        37,
        (37 + 62) / 2.,
        2);
    index().parse(title1).forEach(t -> {
      testBM25.withSegment(Segment.TITLE, t);
      testBM25.withTerm(t, 0);
    });
    index().parse(page1).forEach(t -> {
      testBM25.withSegment(Segment.BODY, t);
      testBM25.withTerm(t, 0);
    });

    Vec resBM25 = testBM25.advance();
    System.err.println(resBM25);

    final double score = Math.log(2.0 / 1) * 2 / (2 + K * (1 - B + B * 38. / (38 + 63) * 2));

    System.err.println(score);
    Assert.assertEquals(0, Double.compare(resBM25.get(0), score));

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
    public Stream<Link> outgoingLinks() {
      return null;
    }

    @Override
    public Stream<Link> incomingLinks() {
      return null;
    }

    @Override
    public Page parent() {
      return null;
    }

    @Override
    public Stream<Page> subpages() {
      return null;
    }

    @Override
    public Stream<CharSequence> sentences(SegmentType type) {
      return null;
    }

    @Override
    public Stream<Term> parse(CharSequence sequence) {
      return null;
    }
  }

}
