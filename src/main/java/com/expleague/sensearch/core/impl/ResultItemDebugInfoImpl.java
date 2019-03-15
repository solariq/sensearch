package com.expleague.sensearch.core.impl;

import com.expleague.sensearch.SenSeArch.ResultItemDebugInfo;

public class ResultItemDebugInfoImpl implements ResultItemDebugInfo {

  private final String uri;
  private final int rank;
  private final double[] features;
  private final String[] featureIds;

  public ResultItemDebugInfoImpl(String uri, int rank, double[] features, String[] featureIds) {
    this.uri = uri;
    this.rank = rank;
    this.features = features;
    this.featureIds = featureIds;
  }

  @Override
  public String uri() {
    return uri;
  }

  @Override
  public int rank() {
    return rank;
  }

  @Override
  public double[] features() {
    return features;
  }

  @Override
  public String[] featureIds() {
    return featureIds;
  }
}
