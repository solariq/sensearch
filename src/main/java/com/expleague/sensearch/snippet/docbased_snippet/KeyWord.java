package com.expleague.sensearch.snippet.docbased_snippet;

import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.core.Term;
import java.util.Objects;

public class KeyWord {
  private Term word;
  private double rank = 0;

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
