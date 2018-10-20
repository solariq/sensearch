package components.searcher;

import components.index.Index;
import components.index.IndexedDocument;
import components.query.Query;
import components.query.term.Term;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Created by sandulmv on 20.10.18.
 * Ranking based on fuzzy set theory
 */
public class
FuzzySearcher implements Searcher {
  private int windowSize;
  private Index index;
  public FuzzySearcher(Index index, int windowSize) {
    this.index = index;
    this.windowSize = windowSize;
  }

  @Override
  public List<IndexedDocument> getRankedDocuments(Query query) {
    final double threshold = query.getTerms().size() / 2.;
    return index.fetchDocuments(query)
        .map(doc -> Pair.of(doc, getFuzzyRank(query, doc)))
        .sorted(Comparator.<Pair<IndexedDocument, Double>>comparingDouble(Pair::getRight).reversed())
        .filter(p -> p.getRight() > threshold)
        .map(Pair::getLeft)
        .collect(Collectors.toList());
  }

  private double getFuzzyRank(Query query, IndexedDocument document) {
    TObjectLongMap<String> termsCounts = new TObjectLongHashMap<>();
    Map<String, TObjectLongMap<String>> termsCooccurrences= new HashMap<>();

    Set<String> rawQueryTerms = query
        .getTerms()
        .stream()
        .map(Term::getRaw)
        .map(CharSequence::toString)
        .collect(Collectors.toSet());
    LinkedList<String> window = new LinkedList<>();
    int currentWindowSize = 0;

    for (String token : document
        .getContent()
        .toString()
        .toLowerCase()
        .split("[\\s.,:;\\-\\n\\t\\r]")) {
      if (rawQueryTerms.contains(token)) {
        for (String neighbour : window) {
          if (!termsCooccurrences.containsKey(token)) {
            termsCooccurrences.put(token, new TObjectLongHashMap<>());
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
    for (Entry<String, TObjectLongMap<String>> coocEntry : coocurrencesMap.entrySet()) {
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
