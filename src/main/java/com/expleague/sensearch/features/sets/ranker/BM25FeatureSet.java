package com.expleague.sensearch.features.sets.ranker;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.core.Term.TermAndDistance;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.features.sets.ranker.WeightedTextFeatureSet.WeightedTextFeatureAccumulator;
import com.expleague.sensearch.query.Query;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class BM25FeatureSet extends FeatureSet.Stub<QURLItem> implements TextFeatureSet {

  private static final FeatureMeta BM25 =
      FeatureMeta.create("bm25", "Title + text bm25", ValueType.VEC);
  private static final FeatureMeta BM25L =
      FeatureMeta.create("bm25l", "Title + text bm25 by lemmas", ValueType.VEC);
  private static final FeatureMeta BM25S =
      FeatureMeta.create("bm25s", "Title + text bm25 by synonyms", ValueType.VEC);
  private static final FeatureMeta BM25W =
      FeatureMeta.create(
          "bm25w", "Title + text bm25 by synonyms, weighted by cos dist", ValueType.VEC);
  private static final FeatureMeta BM25F =
      FeatureMeta.create("bm25f", "field-dependent bm25", ValueType.VEC);
  private static final FeatureMeta WORDS_SHARE =
      FeatureMeta.create(
          "words-share", "Document contained words idf sum normalized by total idf", ValueType.VEC);
  private static final FeatureMeta WORDS_COUNT =
      FeatureMeta.create("words-count", "Words in query", ValueType.VEC);
  private static final FeatureMeta QUERY_LENGTH =
      FeatureMeta.create("query-length", "Query text length in characters", ValueType.VEC);
  private static final FeatureMeta IDF_TOTAL =
      FeatureMeta.create("idf-total", "Total idf sum of query words", ValueType.VEC);

  private static final double K = 1.2;
  private static final double B = 0.75;

  private Set<Term> queryTerms;
  private Set<Term> queryLemmas;
  private Map<Term, Double> querySynWithWeight;
  private Map<Term, Double> querySynLemmaWithWeight = new HashMap<>();
  private Query query;
  private BitSet contains;

  private TextFeatureAccumulator bm25;
  private TextFeatureAccumulator bm25l;
  private TextFeatureAccumulator bm25s;
  private WeightedTextFeatureAccumulator bm25w;
  private BM25FAccumulator bm25f;
  private int collectionSize;

  @Override
  public void accept(QURLItem item) {
    final Query query = item.queryCache();
    if (query.equals(this.query)) {
      contains.clear();
      return;
    }

    this.query = query;
    queryTerms = new HashSet<>(query.terms());
    queryLemmas = query.terms().stream().map(Term::lemma).collect(Collectors.toSet());
    querySynWithWeight =
        query
            .synonymsWithDistance()
            .values()
            .stream()
            .flatMap(List::stream)
            .collect(
                Collectors.toMap(TermAndDistance::term, TermAndDistance::distance, (x, y) -> x));

    querySynLemmaWithWeight.clear();
    Map<Term, List<Entry<Term, Double>>> synsByLemma =
        querySynWithWeight
            .entrySet()
            .stream()
            .collect(Collectors.groupingBy(x -> x.getKey().lemma()));
    synsByLemma.forEach(
        (lemma, terms) -> {
          querySynLemmaWithWeight.put(
              lemma, terms.stream().mapToDouble(Entry::getValue).sum() / terms.size());
        });

    contains = new BitSet(query.terms().size());
  }

  @Override
  public Vec advance() {
    set(BM25, bm25.value(TermType.TERM));
    set(BM25L, bm25l.value(TermType.LEMMA));
    set(BM25S, bm25s.value(TermType.LEMMA));
    set(BM25W, bm25w.value(TermType.LEMMA));
    set(BM25F, bm25f.value(TermType.LEMMA));
    {
      final double totalIdf =
          query.terms().stream().mapToDouble(term -> idf(term, TermType.TERM)).sum() + 1;
      set(IDF_TOTAL, totalIdf);
      set(WORDS_COUNT, query.terms().size());
      set(QUERY_LENGTH, query.text().trim().length());
      set(
          WORDS_SHARE,
          contains
              .stream()
              .mapToObj(query.terms()::get)
              .mapToDouble(term -> idf(term, TermType.TERM))
              .sum()
              / totalIdf);
    }
    return super.advance();
  }

  @Override
  public void withStats(
      int totalLength,
      double averageTotalLength,
      int titleLength,
      double averageTitleLength,
      int contentLength,
      double averageContentLength,
      int indexLength) {
    collectionSize = indexLength;
    bm25 = new BM25Accumulator(totalLength, averageTotalLength);
    bm25l = new BM25Accumulator(totalLength, averageTotalLength);
    bm25s = new BM25Accumulator(totalLength, averageTotalLength);
    bm25w = new BM25WeightedAccumulator(totalLength, averageTotalLength);
    bm25f =
        new BM25FAccumulator(contentLength, averageContentLength, titleLength, averageTitleLength);
  }

  @Override
  public void withSegment(Segment type, Term term) {
    if (queryTerms.contains(term) || queryLemmas.contains(term)) {
      bm25f.accept(type, term);
    }
  }

  @Override
  public void withTerm(Term term, int offset) {
    Term termLemma = term.lemma();
    if (queryTerms.contains(term)) {
      int termIndex = query.terms().indexOf(term);
      contains.set(termIndex);
      bm25.accept(term);
      bm25l.accept(termLemma);
      bm25s.accept(termLemma);
      if (querySynWithWeight.containsKey(term)) {
        bm25w.accept(termLemma, 1 - querySynWithWeight.get(term));
      }
    } else {
      if (queryLemmas.contains(termLemma)) {
        bm25l.accept(termLemma);
      }
      if (querySynLemmaWithWeight.containsKey(termLemma)) {
        bm25s.accept(termLemma);
        bm25w.accept(termLemma, 1 - querySynLemmaWithWeight.get(termLemma));
      }
    }
  }

  private class BM25Accumulator implements TextFeatureAccumulator {

    private final TObjectIntHashMap<Term> freq = new TObjectIntHashMap<>();
    private final double normalizer;

    BM25Accumulator(int pageLen, double avgLen) {
      normalizer = K * (1 - B + B * pageLen / avgLen);
    }

    @Override
    public void accept(Term t) {
      freq.adjustOrPutValue(t, 1, 1);
    }

    @Override
    public double value(TermType type) {
      double[] result = new double[]{0};
      freq.forEachEntry(
          (term, freq) -> {
            result[0] += idf(term, type) * freq / (freq + normalizer);
            return true;
          });
      return result[0];
    }
  }

  private class BM25WeightedAccumulator implements WeightedTextFeatureAccumulator {

    private final TObjectDoubleMap<Term> termWeight = new TObjectDoubleHashMap<>();
    private final TObjectIntMap<Term> freq = new TObjectIntHashMap<>();
    private final double normalizer;

    BM25WeightedAccumulator(int pageLen, double avgLen) {
      normalizer = K * (1 - B + B * pageLen / avgLen);
    }

    @Override
    public void accept(Term t) {
      accept(t, 1);
    }

    @Override
    public void accept(Term term, double weight) {
      freq.adjustOrPutValue(term, 1, 1);
      termWeight.put(term, weight);
    }

    @Override
    public double value(TermType type) {
      double[] result = new double[]{0};
      freq.forEachEntry(
          (term, freq) -> {
            result[0] += termWeight.get(term) * idf(term, type) * freq / (freq + normalizer);
            return true;
          });
      return result[0];
    }
  }

  private class BM25FAccumulator {

    private final Set<Term> terms = new HashSet<>();
    private final TObjectIntHashMap<Term> freqTITLE = new TObjectIntHashMap<>();
    private final TObjectIntHashMap<Term> freqBODY = new TObjectIntHashMap<>();

    private final double normalizerTitle;
    private final double normalizerBody;

    BM25FAccumulator(int pageLen, double avgLen, int titleLen, double avgTitle) {
      normalizerTitle = (1 + B * (titleLen / avgTitle - 1));
      normalizerBody = (1 + B * (pageLen / avgLen - 1));
    }

    public void accept(Segment type, Term term) {
      terms.add(term);
      switch (type) {
        case TITLE:
          freqTITLE.adjustOrPutValue(term, 1, 1);
        case BODY:
          freqBODY.adjustOrPutValue(term, 1, 1);
      }
    }

    public double value(TermType type) {
      double[] result = new double[]{0};

      terms.forEach(
          term -> {
            final double idf = idf(term, type);

            double tf = 0;
            tf += freqTITLE.get(term) / normalizerTitle;
            tf += freqBODY.get(term) / normalizerBody;
            result[0] += tf / (K + tf) * idf;
          });

      return result[0];
    }
  }

  private double idf(Term term, TermType type) {
    switch (type) {
      case TERM:
        return term.documentFreq() == 0
            ? 0
            : Math.log((double) collectionSize / term.documentFreq());
      case LEMMA:
        return term.documentLemmaFreq() == 0
            ? 0
            : Math.log((double) collectionSize / term.documentLemmaFreq());
      default:
        return 0;
    }
  }
}
