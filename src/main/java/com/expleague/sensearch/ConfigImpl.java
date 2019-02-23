package com.expleague.sensearch;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigImpl implements Config {

  private String temporaryIndex = "Index";
  private String webRoot;
  private String embeddingVectors = "vectors.decomp";
  private String myStem = "mystem";
  private String pathToZIP = "data.gzip";
  private String pathToMetrics = "Metrics";
  private String modelPath = "model";
  private String modelFilterPath = "model";
  private int pageSize = 10;
  private int maxFilterItems;
  private boolean buildIndexFlag = false;
  private boolean trainEmbeddingFlag = false;
  private boolean lshNearestFlag = true;

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

  public void setLshNearestFlag(boolean lshNearestFlag) {
    this.lshNearestFlag = lshNearestFlag;
  }

  @Override
  public boolean getLshNearestFlag() {
    return lshNearestFlag;
  }

  @Override
  public int maxFilterItems() {
    return maxFilterItems;
  }

  @Override
  public Path getModelPath() {
    return Paths.get(modelPath);
  }

  @Override
  public Path getFilterModelPath() {
    return Paths.get(modelFilterPath);
  }

  public void setModelPath(String path) {
    this.modelPath = path;
  }
  public void setModelFilterPath(String path) {
    this.modelFilterPath = path;
  }

  public void setMaxFilterItems(int maxFilterItems) {
    this.maxFilterItems = maxFilterItems;
  }

  @Override
  public Path getIndexRoot() {
    return Paths.get(".").resolve(temporaryIndex);
  }

  public void setTemporaryIndex(String temporaryIndex) {
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

  public void setPathToZIP(String pathToZIP) {
    this.pathToZIP = pathToZIP;
  }

  @Override
  public Path getEmbeddingVectors() {
    return Paths.get(embeddingVectors);
  }

  public void setEmbeddingVectors(String embeddingVectors) {
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
