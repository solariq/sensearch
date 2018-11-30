package com.expleague.sensearch.snippet.docbased_snippet;

import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.WordInfo;

public class KeyWord {
  private WordInfo word;
  private double rank;

  public KeyWord(WordInfo word, double rank) {
    this.word = word;
    this.rank = rank;
  }

  public WordInfo getWord() {
    return word;
  }

  public double getRank() {
    return rank;
  }

  public void setRank(double rank) {
    this.rank = rank;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof KeyWord) {
      final LemmaInfo lemma = this.word.lemma();
      final WordInfo oword = ((KeyWord) o).word;
      if (lemma == null || oword.lemma() == null)
        return word.token().equals(oword.token());
      return lemma.lemma().equals(oword.lemma().lemma());
    }
    return false;
  }

  @Override
  public int hashCode() {
    final LemmaInfo lemma = word.lemma();
    if (lemma == null)
      return word.token().hashCode();
    return lemma.lemma().hashCode();
  }
}
