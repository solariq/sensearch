package com.expleague.sensearch.miner.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.query.Query;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class BM25FeatureSet extends FeatureSet.Stub<QURLItem> implements TextFeatureSet {

  public final static FeatureMeta BM25 = FeatureMeta
      .create("bm25", "Title + text bm25", ValueType.VEC);
  public final static FeatureMeta BM25L = FeatureMeta
      .create("bm25l", "Title + text bm25 by lemmas", ValueType.VEC);
  public final static FeatureMeta BM25S = FeatureMeta
      .create("bm25s", "Title + text bm25 by synonyms", ValueType.VEC);

  private static final double K = 1.2;
  private static final double B = 0.75;

  private Set<Term> queryTerms;
  private Set<Term> queryLemmas;
  private Set<Term> querySyn;
  private Query query;

  private TextFeatureAccumulator bm25;
  private TextFeatureAccumulator bm25l;
  private TextFeatureAccumulator bm25s;

  @Override
  public void accept(QURLItem item) {
    final Query query = item.queryCache();
    if (query.equals(this.query))
      return;

    this.query = query;
    queryTerms = new HashSet<>(query.terms());
    queryLemmas = query.terms().stream().map(Term::lemma).collect(Collectors.toSet());
    querySyn = query.synonyms().values().stream().flatMap(List::stream).collect(Collectors.toSet());
  }

  @Override
  public Vec advance() {
    set(BM25, bm25.value());
    set(BM25L, bm25l.value());
    set(BM25S, bm25s.value());
    return super.advance();
  }


  @Override
  public void withStats(int pageLen, double avgLen, int indexLen) {
    bm25 = new BM25Accumulator(pageLen, avgLen, indexLen);
    bm25l = new BM25Accumulator(pageLen, avgLen, indexLen);
    bm25s = new BM25Accumulator(pageLen, avgLen, indexLen);
  }

  @Override
  public void withSegment(Segment type, int length) {
  }

  @Override
  public void withTerm(Term term, int offset) {
    if (queryTerms.contains(term)) {
      bm25.accept(term);
      bm25l.accept(term);
      bm25s.accept(term);
    }
    else {
      if (queryLemmas.contains(term)) {
        bm25l.accept(term);
      }
      if (querySyn.contains(term)) {
        bm25s.accept(term);
      }
    }
  }

  private class BM25Accumulator implements TextFeatureAccumulator {
    private final TObjectIntHashMap<Term> freq = new TObjectIntHashMap<>();
    private final int collectionSize;
    private double normalizer;
    private int pageLen;

    BM25Accumulator(int pageLen, double avgLen, int indexSize) {
      normalizer = K * (1 - B + B * pageLen / avgLen);
      this.collectionSize = indexSize;
      this.pageLen = pageLen;
    }

    @Override
    public void accept(Term t) {
      freq.adjustOrPutValue(t, 1, 1);
    }

    @Override
    public double value() {
      double[] result = new double[]{0};
      freq.forEachEntry(
          (term, freq) -> {
            final int df = term.documentFreq();
            final double idf = df == 0 ? 0 : Math.log((collectionSize - df + 0.5) / (df + 0.5));
            result[0] += df
                    / (pageLen + .0)
                    * (K + 1)
                    * freq
                    / (freq + normalizer)
                    * idf;
            return true;
          });
      return result[0];
    }
  }

}
