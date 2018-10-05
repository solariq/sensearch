package simpleSearch.baseSearch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by sandulmv on 03.10.18.
 */

public class TfIdfRanker extends DocumentRanker {

  public TfIdfRanker(Index index) {
    super(index);
  }

  @Override
  public List<DocumentId> sortDocuments(Query initialQuery, List<TermInfo> termInfoList) {
    Term[] queryTerms = initialQuery.getTermsList().toArray(
        new Term[initialQuery.getTermsList().size()]
    );
    double[] inverseTermFrequencies = new double[initialQuery.getTermsList().size()];
    Map<DocumentId, double[]> termFrequencies = new HashMap<>();

    for (TermInfo termInfo : termInfoList) {
      int termIndex;
      for (termIndex = 0; termIndex < queryTerms.length; ++termIndex) {
        if (queryTerms[termIndex].equals(termInfo.getTerm())) {
          break;
        }
      }
      inverseTermFrequencies[termIndex] = termInfo.getInverseDocumentFrequency();

      for (Map.Entry<DocumentId, Double> entry : termInfo.getTermFrequencies().entrySet()) {
        DocumentId documentId = entry.getKey();
        double termFrequency = entry.getValue();

        if (!termFrequencies.containsKey(documentId)) {
          termFrequencies.put(documentId, new double[queryTerms.length]);
        }

        termFrequencies.get(documentId)[termIndex] = termFrequency;
      }
    }

    normalizeVector(inverseTermFrequencies);

    TreeMap<Double, List<DocumentId>> scoredDocuments = new TreeMap<>(Comparator.reverseOrder());
    for (Map.Entry<DocumentId, double[]> frequencies : termFrequencies.entrySet()) {
      DocumentId documentId = frequencies.getKey();
      normalizeVector(frequencies.getValue());
      double documentScore = dotProduct(frequencies.getValue(), inverseTermFrequencies);
      if (!scoredDocuments.containsKey(documentScore)) {
        scoredDocuments.put(documentScore, new ArrayList<>());
      }

      scoredDocuments.get(documentScore).add(documentId);
    }

    List<DocumentId> rankedIds = new ArrayList<>();
    for (List<DocumentId> documentIds : scoredDocuments.values()) {
      rankedIds.addAll(documentIds);
    }

    return rankedIds;
  }

  private double dotProduct(double[] vec1, double[] vec2) {
    if (vec1.length != vec2.length) {
      return 0;
    }

    double score = 0;
    for (int i = 0; i < vec1.length; ++i) {
      score += vec1[i] * vec2[i];
    }

    return score;
  }

  private void normalizeVector(double[] vector) {
    double vectorNorm = 0;
    for (int i = 0; i < vector.length; ++i) {
      vectorNorm += vector[i] * vector[i];
    }
    vectorNorm = Math.sqrt(vectorNorm);

    for (int i = 0; i < vector.length; ++i) {
      vector[i] /= vectorNorm;
    }
  }
}
