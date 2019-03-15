package com.expleague.sensearch.core.impl;

import com.expleague.sensearch.SenSeArch.SynonymAndScore;
import com.expleague.sensearch.SenSeArch.SynonymInfo;

public class SynonymInfoImpl implements SynonymInfo {

  public SynonymInfoImpl(String query, SynonymAndScore[] queryWordSynonyms) {
    this.query = query;
    this.queryWordSynonyms = queryWordSynonyms;
  }

  private final String query;
  private final SynonymAndScore[] queryWordSynonyms;

  @Override
  public String queryWord() {
    return query;
  }

  @Override
  public SynonymAndScore[] queryWordSynonyms() {
    return queryWordSynonyms;
  }
}
