package com.expleague.sensearch.ranking;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class RankingPhase implements SearchPhase {
  private final Index index;
  private final int windowSize;
  private final int pageSize;

  public RankingPhase(Index index, int windowSize, int pageSize) {
    this.index = index;
    this.windowSize = windowSize;
    this.pageSize = pageSize;
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.query() != null;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    final Query query = whiteboard.query();
    final double threshold = query.getTerms().size() / 2.;
    whiteboard.putResults(index.fetchDocuments(query)
        .map(doc -> Pair.of(doc, getFuzzyRank(query, doc)))
        .sorted(
            Comparator.<Pair<IndexedPage, Double>>comparingDouble(Pair::getRight).reversed())
        .filter(p -> p.getRight() > threshold)
        .skip(whiteboard.pageNo() * pageSize).limit(pageSize)
        .map(Pair::getLeft).toArray(Page[]::new));
  }

  private double getFuzzyRank(Query query, IndexedPage document) {
    TObjectLongMap<String> termsCounts = new TObjectLongHashMap<>();
    Map<String, TObjectLongMap<String>> termsCooccurrences = new HashMap<>();

    Set<String> rawQueryTerms = query
        .getTerms()
        .stream()
        .map(Term::getRaw)
        .map(CharSequence::toString)
        .map(String::toLowerCase)
        .collect(Collectors.toSet());
    LinkedList<String> window = new LinkedList<>();
    int currentWindowSize = 0;

    for (String token : document
        .text()
        .toString()
        .toLowerCase()
        .split("[\\s.,:;\\-\\n\\t\\r]")) {
      if (rawQueryTerms.contains(token)) {
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

      if (currentWindowSize >= this.windowSize) {
        window.pollFirst();
      } else {
        ++currentWindowSize;
      }
    }

    return calculateScore(termsCounts, termsCooccurrences);
  }

  private double calculateScore(TObjectLongMap<String> termsCounts,
                                Map<String, TObjectLongMap<String>> coocurrencesMap) {
    double totalScore = 0.;
    for (Map.Entry<String, TObjectLongMap<String>> coocEntry : coocurrencesMap.entrySet()) {
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
}
