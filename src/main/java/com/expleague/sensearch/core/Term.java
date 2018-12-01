package com.expleague.sensearch.core;

import java.util.stream.Stream;

public interface Term {
  CharSequence text();

  Term lemma();
  Stream<Term> synonyms();

  int documentFreq();
  int freq();
}
