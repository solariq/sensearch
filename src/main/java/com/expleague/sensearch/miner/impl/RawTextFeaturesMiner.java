package com.expleague.sensearch.miner.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.miner.Features;
import com.expleague.sensearch.miner.FeaturesMiner;
import com.expleague.sensearch.query.Query;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RawTextFeaturesMiner implements FeaturesMiner {
  // BM25 parameters
  private static final double K = 1.2;
  private static final double B = 0.75;

  private final Index index;

  public RawTextFeaturesMiner(Index index) {
    this.index = index;
  }

  interface TextFeatureAccumulator {
    void accept(Term term);

    double value();
  }

  @Override
  public Features extractFeatures(Query query, Page page) {
    final Set<Term> queryTerms = new HashSet<>(query.terms());
    final Set<Term> queryLemmas =
        query.terms().stream().map(Term::lemma).collect(Collectors.toSet());
    final Set<Term> querySyn =
        query.synonyms().values().stream().flatMap(List::stream).collect(Collectors.toSet());

    final int pageSize = (int) index.parse(page.title() + " " + page.text()).count();
    TextFeatureAccumulator bm25 = new BM25Accumulator(pageSize);
    TextFeatureAccumulator bm25l = new BM25Accumulator(pageSize);
    TextFeatureAccumulator bm25s = new BM25Accumulator(pageSize);
    index
        .parse(page.title() + " " + page.text())
        .forEach(
            term -> {
              if (queryTerms.contains(term)) {
                bm25.accept(term);
                bm25l.accept(term);
                bm25s.accept(term);
              } else {
                if (queryLemmas.contains(term)) {
                  bm25l.accept(term);
                }
                if (querySyn.contains(term)) {
                  bm25s.accept(term);
                }
              }
            });

    return new TextFeaturesImpl(bm25.value(), bm25l.value(), bm25s.value());
  }

  private static class TextFeaturesImpl implements Features {

    private final double bm25;
    private final double bm25l;
    private final double bm25s;

    public TextFeaturesImpl(double bm25, double bm25l, double bm25s) {
      this.bm25 = bm25;
      this.bm25l = bm25l;
      this.bm25s = bm25s;
    }

    @Override
    public Vec features() {
      return new ArrayVec(bm25, bm25l, bm25s);
    }
  }

  private class BM25Accumulator implements TextFeatureAccumulator {
    private final int pageSize;
    private final TObjectIntHashMap<Term> freq = new TObjectIntHashMap<>();

    public BM25Accumulator(int pageSize) {
      this.pageSize = pageSize;
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
            final double idf = df == 0 ? 0 : Math.log((index.size() - df + 0.5) / (df + 0.5));
            result[0] +=
                df
                    * (K + 1)
                    * freq
                    / (freq + K * (1 - B + B * pageSize / index.averagePageSize()))
                    * idf;
            return true;
          });
      return result[0];
    }
  }

  // fuzzy rank parameter
  //  private static final int CONTEXT_WINDOW_SIZE = 4;
  //  private double fuzzyRank(String[] rawTerms, String[] contentTokens) {
  //    Set<String> rawTermsSet = Stream.of(rawTerms).collect(Collectors.toSet());
  //    TObjectLongMap<String> termsCounts = new TObjectLongHashMap<>();
  //    Map<String, TObjectLongMap<String>> termsCooccurrences = new HashMap<>();
  //
  //    int currentWindowSize = 0;
  //    LinkedList<String> window = new LinkedList<>();
  //    for (String token : contentTokens) {
  //      if (rawTermsSet.contains(token)) {
  //        for (String neighbour : window) {
  //          if (!termsCooccurrences.containsKey(token)) {
  //            termsCooccurrences.put(token, new TObjectLongHashMap<>());
  //          }
  //          if (!termsCooccurrences.get(token).containsKey(neighbour)) {
  //            termsCooccurrences.get(token).put(neighbour, 0);
  //          }
  //          termsCooccurrences.get(token).increment(neighbour);
  //        }
  //      } else {
  //        window.addLast(token);
  //      }
  //
  //      if (!termsCounts.containsKey(token)) {
  //        termsCounts.put(token, 0L);
  //      }
  //      termsCounts.increment(token);
  //
  //      if (currentWindowSize >= CONTEXT_WINDOW_SIZE) {
  //        window.pollFirst();
  //      } else {
  //        ++currentWindowSize;
  //      }
  //    }
  //
  //    double totalScore = 0.;
  //    for (Map.Entry<String, TObjectLongMap<String>> coocEntry : termsCooccurrences.entrySet()) {
  //      double wordScore = 1.;
  //      double keyCount = termsCounts.get(coocEntry.getKey());
  //
  //      TObjectLongMap<String> cooccurrences = coocEntry.getValue();
  //      for (String neighbour : cooccurrences.keySet()) {
  //        double neighbourCount = termsCounts.get(neighbour);
  //        double coocurrenncesCount = cooccurrences.get(neighbour);
  //        // keyCount >= 1, neighbourCount >= 1, coocurrenceCount < keyCount + neighbourCount
  //        wordScore *= 1. - coocurrenncesCount / (keyCount + neighbourCount - coocurrenncesCount);
  //      }
  //
  //      totalScore += (1 - wordScore);
  //    }
  //
  //    return totalScore;
  //  }

}
