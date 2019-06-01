package com.expleague.sensearch.web.suggest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.expleague.sensearch.core.Term;

public interface Suggester {
  List<String> getSuggestions(String searchString);
  default String getName() {
    return "Some suggestor";
  }
  
  default String termsToString(Term[] terms) {
    return Arrays.stream(terms)
    .map(Term::text)
    .collect(Collectors.joining(" "));
  }
  
  default String termsToString(List<Term> terms) {
    return terms.stream()
        .map(Term::text)
        .collect(Collectors.joining(" "));
  }
  
  default String wordsConcat(String pref, String suff) {
    return pref.isEmpty() ? suff : (pref + " " + suff);
  }
}
