package com.expleague.sensearch.snippet.docbased_snippet;

import com.expleague.sensearch.query.term.Term;

public class KeyWord {

  private Term word;
  private double rank;

  public KeyWord(Term word, double rank) {
    this.word = word;
    this.rank = rank;
  }

  public Term getWord() {
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
      return this.word.getNormalized().equals(((KeyWord) o).word.getNormalized());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return word.getNormalized().hashCode();
  }
}
