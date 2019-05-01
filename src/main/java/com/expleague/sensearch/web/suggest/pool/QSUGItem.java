package com.expleague.sensearch.web.suggest.pool;

import com.expleague.ml.meta.GroupedDSItem;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"suggestion",
  "isPositive",
  "intersect",
  "links",
  "prob_coef",
  "cosine"})
public class QSUGItem extends GroupedDSItem.Stub {
  public final String suggestion;

  public final boolean isPositive;
  public final int intersectionLength;
  public final int incomingLinksCount;
  public final double probabilisticCoeff;
  public final double cosine;

  @JsonCreator
  public QSUGItem(
      @JsonProperty("suggestion") String suggestion,
      @JsonProperty("intersect") int intersect,
      @JsonProperty("links") int links,
      @JsonProperty("prob_coef") double coef,
      @JsonProperty("cosine") double cosine,
      @JsonProperty("isPositive") boolean isPositive) {
    super();
    this.suggestion = suggestion;
    this.intersectionLength = intersect;
    this.incomingLinksCount = links;
    this.probabilisticCoeff = coef;
    this.cosine = cosine;
    
    this.isPositive = isPositive;
  }

  @JsonProperty("suggestion")
  public String getSuggestion() {
    return suggestion;
  }

  @JsonProperty("isPositive")
  public boolean isPositive() {
    return isPositive;
  }

  @JsonProperty("intersect")
  public int getIntersectionLength() {
    return intersectionLength;
  }

  @JsonProperty("links")
  public int getIncomingLinksCount() {
    return incomingLinksCount;
  }

  @JsonProperty("prob_coef")
  public double getProbabilisticCoeff() {
    return probabilisticCoeff;
  }

  @JsonProperty("cosine")
  public double getCosine() {
    return cosine;
  }

  @Override
  public String id() {
    return suggestion;
  }

  @Override
  public String groupId() {
    return "group";
  }
  
}
