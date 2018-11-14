package com.expleague.sensearch.core.impl;

import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.SenSeArch.ResultPage;

public class ResultPageImpl implements ResultPage {

  private final int number;
  private final int totalResults;
  private final ResultItem[] results;
  private final ResultItem[] googleResults;

  public ResultPageImpl(
      int number, int totalResults, ResultItem[] results, ResultItem[] googleResults) {
    this.number = number;
    this.totalResults = totalResults;
    this.results = results;
    this.googleResults = googleResults;
  }

  @Override
  public int number() {
    return number;
  }

  @Override
  public int totalResultsFound() {
    return totalResults;
  }

  @Override
  public ResultItem[] results() {
    return results;
  }

  @Override
  public ResultItem[] googleResults() {
    return googleResults;
  }
}
