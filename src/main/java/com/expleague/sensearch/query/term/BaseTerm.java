package com.expleague.sensearch.query.term;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.WordInfo;

public class BaseTerm implements Term {

  private WordInfo wordInfo;
  private Vec vector;

  public BaseTerm(WordInfo wordInfo) {
    this.wordInfo = wordInfo;
  }

  public BaseTerm(WordInfo wordInfo, Vec vector) {
    this.wordInfo = wordInfo;
    this.vector = vector;
  }

  @Override
  public CharSequence getRaw() {
    return wordInfo.token();
  }

  @Override
  public CharSequence getNormalized() {
    if (wordInfo.lemma() == null) return getRaw();
    return this.wordInfo.lemma().lemma();
  }

  @Override
  public Vec getVector() {
    return this.vector;
  }

  @Override
  public LemmaInfo getLemma() {
    return this.wordInfo.lemma();
  }
}
