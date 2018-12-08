package com.expleague.sensearch.core;

import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public interface Term {
  CharSequence text();

  // lemma of lemma == lemma
  Term lemma();
  Stream<Term> synonyms();

  int documentFreq();
  int freq();

  // Can be null if lemma can not be determined
  @Nullable
  PartOfSpeech partOfSpeech();
}
