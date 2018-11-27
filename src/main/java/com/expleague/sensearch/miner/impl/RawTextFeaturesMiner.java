package com.expleague.sensearch.miner.impl;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.miner.Features;
import com.expleague.sensearch.miner.FeaturesMiner;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RawTextFeaturesMiner implements FeaturesMiner {

  private static final Pattern SIMPLE_SPLITTER = Pattern.compile("[^а-яёa-z0-9]+");
  // fuzzy rank parameter
  private static final int CONTEXT_WINDOW_SIZE = 4;

  // BM25 parameters
  private static final double K = 1.2;
  private static final double B = 0.75;

  private final Index index;

  public RawTextFeaturesMiner(Index index) {
    this.index = index;
  }

  @Override
  public Features extractFeatures(Query query, Page page) {
    String[] contentTokens = SIMPLE_SPLITTER
        .split((page.title() + " " + page.text()).toLowerCase());
    String[] rawTerms = query.getTerms()
        .stream()
        .map(Term::getRaw)
        .map(CharSequence::toString)
        .map(String::toLowerCase)
        .toArray(String[]::new);

    return new TextFeaturesImpl(
        0,//bm25Rank(query.getTerms(), contentTokens),
        fuzzyRank(rawTerms, contentTokens),
        lmRank(rawTerms, contentTokens),
        dlhRank(rawTerms, contentTokens),
        dllhRank(rawTerms, contentTokens)
    );
  }

  private String[] getRawTerms(List<Term> terms) {
    return terms
        .stream()
        .map(Term::getRaw)
        .map(CharSequence::toString)
        .map(String::toLowerCase)
        .toArray(String[]::new);
  }

  private double bm25Rank(List<Term> terms, String[] contentTokens) {
    String[] rawTerms = getRawTerms(terms);
    double[] idf = new double[rawTerms.length];
    double[] rawCounts = new double[rawTerms.length];

    int indexSize = index.size();
    for (int i = 0; i < rawTerms.length; ++i) {
      int pagesWithTerm = index.documentFrequency(terms.get(i));
      idf[i] = pagesWithTerm == 0 ? 0 :
          Math.log((indexSize - pagesWithTerm + 0.5) / (pagesWithTerm + 0.5));
      idf[i] = idf[i] < 0 ? 0 : idf[i];
    }

    for (String token : contentTokens) {
      for (int i = 0; i < rawTerms.length; ++i) {
        if (rawTerms[i].equals(token)) {
          rawCounts[i] += 1;
        }
      }
    }

    double totalScore = 0;
    int pageSize = contentTokens.length;
    double averagePageSize = index.averagePageSize();
    for (int i = 0; i < idf.length; ++i) {
      if (idf[i] > 0) {
        double tf = rawCounts[i] / pageSize;
        totalScore += idf[i] * (K + 1) * tf / (tf + K * (1 - B + B * pageSize / averagePageSize));
      }
    }
    return totalScore;
  }

  private double fuzzyRank(String[] rawTerms, String[] contentTokens) {
    Set<String> rawTermsSet = Stream.of(rawTerms).collect(Collectors.toSet());
    TObjectLongMap<String> termsCounts = new TObjectLongHashMap<>();
    Map<String, TObjectLongMap<String>> termsCooccurrences = new HashMap<>();

    int currentWindowSize = 0;
    LinkedList<String> window = new LinkedList<>();
    for (String token : contentTokens) {
      if (rawTermsSet.contains(token)) {
        for (String neighbour : window) {
          if (!termsCooccurrences.containsKey(token)) {
            termsCooccurrences.put(token, new TObjectLongHashMap<>());
          }
          if (!termsCooccurrences.get(token).containsKey(neighbour)) {
            termsCooccurrences.get(token).put(neighbour, 0);
          }
          termsCooccurrences.get(token).increment(neighbour);
        }
      } else {
        window.addLast(token);
      }

      if (!termsCounts.containsKey(token)) {
        termsCounts.put(token, 0L);
      }
      termsCounts.increment(token);

      if (currentWindowSize >= CONTEXT_WINDOW_SIZE) {
        window.pollFirst();
      } else {
        ++currentWindowSize;
      }
    }

    double totalScore = 0.;
    for (Map.Entry<String, TObjectLongMap<String>> coocEntry : termsCooccurrences.entrySet()) {
      double wordScore = 1.;
      double keyCount = termsCounts.get(coocEntry.getKey());

      TObjectLongMap<String> cooccurrences = coocEntry.getValue();
      for (String neighbour : cooccurrences.keySet()) {
        double neighbourCount = termsCounts.get(neighbour);
        double coocurrenncesCount = cooccurrences.get(neighbour);
        // keyCount >= 1, neighbourCount >= 1, coocurrenceCount < keyCount + neighbourCount
        wordScore *= 1. - coocurrenncesCount / (keyCount + neighbourCount - coocurrenncesCount);
      }

      totalScore += (1 - wordScore);
    }

    return totalScore;
  }

  private double dllhRank(String[] rawTerms, String[] contentTokens) {
    return 0;
  }

  private double dlhRank(String[] rawTerms, String[] contentTokens) {
    return 0;
  }

  private double lmRank(String[] rawTerms, String[] contentTokens) {
    return 0;
  }

  private static class TextFeaturesImpl implements Features {

    private final double bm25;
    private final double fuzzy;
    private final double lm;
    private final double dlh;
    private final double dllh;

    public TextFeaturesImpl(double bm25, double fuzzy, double lm, double dlh, double dllh) {
      this.bm25 = bm25;
      this.fuzzy = fuzzy;
      this.lm = lm;
      this.dlh = dlh;
      this.dllh = dllh;
    }

    @Override
    public double bm25() {
      return bm25;
    }

    @Override
    public double fuzzy() {
      return fuzzy;
    }

    @Override
    public double lm() {
      return lm;
    }

    @Override
    public double dlh() {
      return dlh;
    }

    @Override
    public double dllh() {
      return dllh;
    }
  }
}
