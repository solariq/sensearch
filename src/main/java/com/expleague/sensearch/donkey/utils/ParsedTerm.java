package com.expleague.sensearch.donkey.utils;

import com.expleague.commons.seq.CharSeq;
import com.expleague.sensearch.core.PartOfSpeech;
import javax.annotation.Nullable;

public class ParsedTerm {

  private static final String LEMMA_SUFFIX = "$";

  private final int wordId;
  private final CharSeq word;

  private final int lemmaId;
  private final CharSeq lemma;

  private final PartOfSpeech posTag;

  protected ParsedTerm(int wordId, CharSeq word, int lemmaId, CharSeq lemma, PartOfSpeech posTag) {
    this.wordId = wordId;
    this.word = word;
    this.lemmaId = lemmaId;
    this.lemma = lemma;
    this.posTag = posTag;
  }

  public static ParsedTerm create(int wordId, CharSeq word, int lemmaId, CharSeq lemma, PartOfSpeech posTag) {
    return new ParsedTerm(wordId, word, lemmaId, lemma, posTag);
  }

  public CharSeq lemma() {
    return lemma;
  }

  public int lemmaId() {
    return lemmaId;
  }

  public CharSeq word() {
    return word;
  }

  public int wordId() {
    return wordId;
  }

  @Nullable
  public PartOfSpeech posTag() {
    return posTag;
  }

  public boolean hasPosTag() {
    return posTag != null;
  }

  public boolean hasLemma() {
    return lemmaId != -1;
  }
}
