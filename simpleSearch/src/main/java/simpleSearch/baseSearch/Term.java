package simpleSearch.baseSearch;

import java.util.List;

/**
 * Created by sandulmv on 03.10.18.
 */
public class Term {
  public enum PartOfSpeech {
    NOUN,
    VERB
  }

  private String rawTerm;
  private String normalizedWord;
  private List<String> otherForms;
  private PartOfSpeech partOfSpeech;
}
