package com.expleague.sensearch.donkey.term;

import com.expleague.sensearch.core.PartOfSpeech;
import javax.annotation.Nullable;

public class ParsedTerm {

  private static final String LEMMA_SUFFIX = "$";

  private final Token termToken;
  private final Token lemmaToken;
  private final PartOfSpeech posTag;

  public ParsedTerm(Token termToken, Token lemmaToken, PartOfSpeech posTag) {
    this.termToken = termToken;
    this.lemmaToken = lemmaToken;
    this.posTag = posTag;
  }

  public ParsedTerm(Token termToken) {
    this.termToken = termToken;
    this.lemmaToken = null;
    this.posTag = null;
  }

  @Nullable
  public CharSequence lemma() {
    return lemmaToken == null ? null : lemmaToken.text();
  }

  public int lemmaId() {
    return lemmaToken == null ? -1 : lemmaToken.id();
  }

  public CharSequence word() {
    return termToken.text();
  }

  public int wordId() {
    return termToken.id();
  }

  @Nullable
  public PartOfSpeech posTag() {
    return posTag;
  }

  public boolean hasPosTag() {
    return posTag != null;
  }

  public boolean hasLemma() {
    return lemmaToken != null;
  }
}
