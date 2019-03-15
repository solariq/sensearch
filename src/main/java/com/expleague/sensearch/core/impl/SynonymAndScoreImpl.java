package com.expleague.sensearch.core.impl;

import com.expleague.sensearch.SenSeArch.SynonymAndScore;

public class SynonymAndScoreImpl implements SynonymAndScore {

  private final double score;
  private final String synonym;

  public SynonymAndScoreImpl(double score, String synonym) {
    this.score = score;
    this.synonym = synonym;
  }

  @Override
  public double score() {
    return score;
  }

  @Override
  public String synonym() {
    return synonym;
  }
}
