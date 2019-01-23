package com.expleague.sensearch;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigImpl implements Config {

  private String temporaryDocuments;
  private String temporaryIndex;
  private String webRoot;
  private String embeddingVectors;
  private String myStem;
  private String pathToZIP;
  private String pathToMetrics;
  private int pageSize = 10;
  private boolean buildIndexFlag;
  private boolean trainEmbeddingFlag;

  @Override
  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  @Override
  public boolean getBuildIndexFlag() {
    return buildIndexFlag;
  }

  private void setBuildIndexFlag(boolean buildIndexFlag) {
    this.buildIndexFlag = buildIndexFlag;
  }

  private void setTrainEmbeddingFlag(boolean trainEmbeddingFlag) {
    this.trainEmbeddingFlag = trainEmbeddingFlag;
  }

  @Override
  public boolean getTrainEmbeddingFlag() {
    return trainEmbeddingFlag;
  }

  @Override
  public Path getTemporaryDocuments() {
    return Paths.get(temporaryDocuments);
  }

  private void setTemporaryDocuments(String temporaryDocuments) {
    this.temporaryDocuments = temporaryDocuments;
  }

  @Override
  public Path getTemporaryIndex() {
    return Paths.get(".").resolve(temporaryIndex);
  }

  private void setTemporaryIndex(String temporaryIndex) {
    this.temporaryIndex = temporaryIndex;
  }

  @Override
  public String getWebRoot() {
    return webRoot;
  }

  private void setWebRoot(String webRoot) {
    this.webRoot = webRoot;
  }

  @Override
  public Path getMyStem() {
    return Paths.get(myStem);
  }

  private void setMyStem(String myStem) {
    this.myStem = myStem;
  }

  @Override
  public Path getPathToZIP() {
    return Paths.get(pathToZIP);
  }

  private void setPathToZIP(String pathToZIP) {
    this.pathToZIP = pathToZIP;
  }

  @Override
  public String getEmbeddingVectors() {
    return embeddingVectors;
  }

  private void setEmbeddingVectors(String embeddingVectors) {
    this.embeddingVectors = embeddingVectors;
  }

  @Override
  public Path getPathToMetrics() {
    return Paths.get(pathToMetrics);
  }

  private void setPathToMetrics(String pathToMetrics) {
    this.pathToMetrics = pathToMetrics;
  }
}
