package simpleSearch.baseSearch;

import java.util.Map;
import java.util.Set;

/**
 * Created by sandulmv on 05.10.18.
 */
public class TermInfo {
  private Term term;
  private double inverseDocumentFrequency;
  private Map<DocumentId, Double> termFrequencies;

  public Term getTerm() {
    return term;
  }

  public double getInverseDocumentFrequency() {
    return inverseDocumentFrequency;
  }

  public Map<DocumentId, Double> getTermFrequencies() {
    return termFrequencies;
  }

  public TermInfo(Term term, double inverseDocumentFrequency,
      Map<DocumentId, Double> termFrequencies) {
    this.term = term;
    this.inverseDocumentFrequency = inverseDocumentFrequency;
    this.termFrequencies = termFrequencies;
  }
}
