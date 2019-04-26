package com.expleague.sensearch.snippet.docbasedsnippet;

import com.expleague.sensearch.core.Term;
import java.util.Objects;

public class KeyWord {

  private final Term word;
  private double rank;

  public KeyWord(Term word) {
    this.word = word;
  }

  public KeyWord(Term word, double rank) {
    this.word = word;
    this.rank = rank;
  }

  public Term word() {
    return word;
  }

  public double rank() {
    return rank;
  }

  public void setRank(double rank) {
    this.rank = rank;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof KeyWord) {
      KeyWord oKeyWord = (KeyWord) o;
      return word == oKeyWord.word;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(word);
  }
}
