package com.expleague.sensearch.query.term;

import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.WordInfo;

public class BaseTerm implements Term {

  private WordInfo wordInfo;

  public BaseTerm(WordInfo wordInfo) {
    this.wordInfo = wordInfo;
  }

  @Override
  public CharSequence getRaw() {
    return wordInfo.token();
  }

  @Override
  public CharSequence getNormalized() {
    if (wordInfo.lemma() == null) {
      return getRaw();
    }
    return this.wordInfo.lemma().lemma();
  }

  @Override
  public LemmaInfo getLemma() {
    return this.wordInfo.lemma();
  }
}
