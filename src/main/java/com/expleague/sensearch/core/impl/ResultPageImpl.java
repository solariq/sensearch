package com.expleague.sensearch.core.impl;

import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.SenSeArch.ResultPage;

public class ResultPageImpl implements ResultPage {

  private final String query;
  private final int number;
  private final int totalResults;
  private final ResultItem[] results;
  private final ResultItem[] debugResults;

  public ResultPageImpl(
      String query,
      int number,
      int totalResults,
      ResultItem[] results,
      ResultItem[] debugResults) {
    this.query = query;
    this.number = number;
    this.totalResults = totalResults;
    this.results = results;
    this.debugResults = debugResults;
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
  public String query() {
    return query;
  }

  @Override
  public ResultItem[] results() {
    return results;
  }

  @Override
  public ResultItem[] debugResults() {
    return debugResults;
  }
}
