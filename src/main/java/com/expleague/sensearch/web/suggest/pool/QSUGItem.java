package com.expleague.sensearch.web.suggest.pool;

import com.expleague.ml.meta.GroupedDSItem;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
  "suggestion",
  "intersect",
  "usedPhraseLength",
  "qc length",
  "links",
  "prob_coef",
  "cosine",
  "tfIdfSum",
  "tfIdfCosine",
  "MalletCosine",
  "vectorSumLength",
  "isPositive",})
public class QSUGItem extends GroupedDSItem.Stub {
  public final String suggestion;

  public final boolean isPositive;
  public final int intersectionLength;
  public final int usedPhraseLength;
  public final int qcLength;
  public final int incomingLinksCount;
  public final double probabilisticCoeff;
  public final double cosine;
  public final double tfidfWeightedCosine;
  public final double malletCosine;
  public final double tfidfSum;
  public final double vectorSumLength;

  @JsonCreator
  public QSUGItem(
      @JsonProperty("suggestion") String suggestion,
      @JsonProperty("intersect") int intersect,
      @JsonProperty("usedPhraseLength") int phraseLength,
      @JsonProperty("qc length") int qcLength,
      @JsonProperty("links") int links,
      @JsonProperty("prob_coef") double coef,
      @JsonProperty("cosine") double cosine,
      @JsonProperty("tfIdfCosine") double tfIdfCosine,
      @JsonProperty("tfIdfSum") double tfIdfSum,
      @JsonProperty("MalletCosine") double malletCosine,
      @JsonProperty("vectorSumLength") double vectorSumLength,
      @JsonProperty("isPositive") boolean isPositive) {
    super();
    this.suggestion = suggestion;
    this.intersectionLength = intersect;
    this.usedPhraseLength = phraseLength;
    this.qcLength = qcLength;
    this.incomingLinksCount = links;
    this.probabilisticCoeff = coef;
    this.cosine = cosine;
    this.tfidfWeightedCosine = tfIdfCosine;
    this.malletCosine = malletCosine;
    this.tfidfSum = tfIdfSum;
    this.vectorSumLength = vectorSumLength;
    
    this.isPositive = isPositive;
  }

  @JsonProperty("tfIdfCosine")
  public double getTfidfWeightedCosine() {
    return tfidfWeightedCosine;
  }

  @JsonProperty("MalletCosine")
  public double getMalletCosine() {
    return malletCosine;
  }

  @JsonProperty("tfIdfSum")
  public double getTfidfSum() {
    return tfidfSum;
  }

  @JsonProperty("vectorSumLength")
  public double getVectorSumLength() {
    return vectorSumLength;
  }

  public QSUGItem asPositive() {
    return new QSUGItem(
        suggestion, 
        intersectionLength, 
        usedPhraseLength, 
        qcLength, 
        incomingLinksCount, 
        probabilisticCoeff, 
        cosine, 
        tfidfWeightedCosine, 
        tfidfSum, 
        malletCosine, 
        vectorSumLength, 
        true);
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
