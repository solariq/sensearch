package com.expleague.sensearch.snippet.docbased_snippet;

public class KeyWord {
  private CharSequence word;
  private double rank;

  public KeyWord(CharSequence word, double rank) {
    this.word = word;
    this.rank = rank;
  }

  public CharSequence getWord() {
    return word;
  }

  public double getRank() {
    return rank;
  }
}
