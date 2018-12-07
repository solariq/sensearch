package com.expleague.sensearch.core;

import com.expleague.commons.text.lemmer.PartOfSpeech;
import java.util.stream.Stream;

public interface Term {
  CharSequence text();

  // lemma of lemma == lemma
  Term lemma();
  Stream<Term> synonyms();

  int documentFreq();
  int freq();

  PartOfSpeech partOfSpeech();
}
