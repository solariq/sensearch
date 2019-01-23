package com.expleague.sensearch.core;

import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public interface Term {
  CharSequence text();

  // lemma of lemma == lemma
  Term lemma();

  Stream<Term> synonyms();

  int documentFreq();

  /**
   * @return number of documents which contain at least one term having the same {@link #lemma()} as
   * this term
   */
  int documentLemmaFreq();

  int freq();

  // Can be null if lemma can not be determined
  @Nullable
  PartOfSpeech partOfSpeech();
}
