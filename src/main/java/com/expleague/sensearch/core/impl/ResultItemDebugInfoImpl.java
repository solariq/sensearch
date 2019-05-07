package com.expleague.sensearch.core.impl;

import com.expleague.sensearch.SenSeArch.ResultItemDebugInfo;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ResultItemDebugInfoImpl implements ResultItemDebugInfo {

  private final String uri;
  private final int rankPlace;
  private final int filterPlace;

  private final double filterScore;
  private final double rankScore;

  private final double[] filterFeatures;
  private final String[] filterFeatureIds;

  private final double[] rankFeatures;
  private final String[] rankFeatureIds;

  public ResultItemDebugInfoImpl(
      String uri,
      int rankPlace,
      int filterPlace,
      double filterScore,
      double rankScore,
      double[] filterFeatures,
      String[] filterFeatureIds,
      double[] rankFeatures,
      String[] rankFeatureIds) {
    this.uri = uri;
    this.rankPlace = rankPlace;
    this.filterPlace = filterPlace;
    this.filterScore = filterScore;
    this.rankScore = rankScore;
    this.filterFeatures = filterFeatures;
    this.filterFeatureIds = filterFeatureIds;
    this.rankFeatures = rankFeatures;
    this.rankFeatureIds = rankFeatureIds;
  }

  @Override
  @JsonProperty("uri")
  public String uri() {
    return uri;
  }

  @Override
  @JsonProperty("rankPlace")
  public int rankPlace() {
    return rankPlace;
  }

  @Override
  @JsonProperty("filterPlace")
  public int filterPlace() {
    return filterPlace;
  }

  @Override
  @JsonProperty("filterScore")
  public double filterScore() {
    return filterScore;
  }

  @Override
  @JsonProperty("rankScore")
  public double rankScore() {
    return rankScore;
  }

  @JsonProperty("rankFeatures")
  public double[] rankFeatures() {
    return rankFeatures;
  }

  @Override
  @JsonProperty("rankFeatureIds")
  public String[] featureIds() {
    return rankFeatureIds;
  }

  @Override
  @JsonProperty("filterFeatures")
  public double[] filterFeatures() {
    return filterFeatures;
  }

  @Override
  @JsonProperty("filterFeatureIds")
  public String[] filterFeatureIds() {
    return filterFeatureIds;
  }
}
