package com.expleague.sensearch.miner.features;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.query.Query;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class HHFeatureSet extends FeatureSet.Stub<QURLItem> implements TextFeatureSet {

  private static final FeatureMeta HHP = FeatureMeta
      .create("hhp", "Title + text hhp", ValueType.VEC);
  private static final FeatureMeta HHPL = FeatureMeta
      .create("lemma-hhp", "Title + text hhp by lemma", ValueType.VEC);

  private static final double Z = 1.75;

  private Query query;
  private Set<Term> queryTerms;
  private final Map<Term, TreeSet<Integer>> termPositions = new HashMap<>();
  private final Map<Term, Double> idf = new HashMap<>();
  private final Map<Term, Double> idfLemma = new HashMap<>();

  @Override
  public void accept(QURLItem item) {
    final Query query = item.queryCache();
    if (query.equals(this.query)) {
      return;
    }

    this.query = query;
    queryTerms = new HashSet<>(query.terms());

  }

  @Override
  public void withStats(int totalLength, double averageTotalLength, int titleLength,
      double averageTitleLength, int contentLength,
      double averageContentLength,
      int indexLength) {
    termPositions.clear();
    queryTerms.forEach(term -> {
      final int df = term.documentFreq();
      final double idf = df == 0 ? 0 : Math.log((double) indexLength / df);
      this.idf.put(term, idf);

      final int dfL = term.documentLemmaFreq();
      final double idfL = dfL == 0 ? 0 : Math.log((double) indexLength / dfL);
      this.idfLemma.put(term.lemma(), idfL);
    });
  }

  @Override
  public void withSegment(Segment type, Term t) {
    // TODO Auto-generated method stub
  }

  @Override
  public void withTerm(Term t, int offset) {
    termPositions.putIfAbsent(t, new TreeSet<>());
    termPositions.get(t).add(offset);

    termPositions.putIfAbsent(t.lemma(), new TreeSet<>());
    termPositions.get(t.lemma()).add(offset);

  }

  @Override
  public Vec advance() {
    set(HHP, hhProximty(TermType.TERM));
    set(HHPL, hhProximty(TermType.LEMMA));
    return super.advance();
  }

  private double frac(Term term, Integer neighPos, int center,
      TermType type) {

    if (neighPos == null) {
      return 0;
    }

    double termIDF = 0;

    switch (type) {
      case TERM:
        termIDF = idf.get(term);
        break;
      case LEMMA:
        termIDF = idfLemma.get(term) ;
    }

    return termIDF / Math.pow(Math.abs(neighPos - center), Z);
  }

  private double tc(Term t, int p, TermType type) {
    double res = 0;

    for (Term term : queryTerms) {
      Term tmpTerm = null;
      switch (type) {
        case TERM:
          tmpTerm = term;
          break;
        case LEMMA:
          tmpTerm = term.lemma();
      }
      TreeSet<Integer> positions = termPositions.get(tmpTerm);
      if (positions == null) {
        continue;
      }

      res += (frac(tmpTerm, positions.lower(p), p, type) +
          frac(tmpTerm, positions.higher(p), p, type))
          * (t.equals(tmpTerm) ? 0.25 : 1);
    }

    return res;
  }

  private double atc(Term t, TermType type) {
    double res = 0;

    if (!termPositions.containsKey(t)) {
      return res;
    }

    for (Integer p : termPositions.get(t)) {
      res += tc(t, p, type);
    }

    return res;
  }

  private double hhProximty(TermType type) {
    double sum = 0;
    switch (type) {
      case TERM:
        for (Term t : queryTerms) {
          sum += atc(t, type) * idf.get(t);
        }
      case LEMMA:
        for (Term t : queryTerms) {
          sum += atc(t.lemma(), type) * idfLemma.get(t.lemma());
        }
    }

    return Math.log(1 + sum);
  }
}
