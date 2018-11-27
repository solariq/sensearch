package com.expleague.sensearch;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Config implements ConfigJson {

  private String temporaryDocuments;

  private String temporaryBigrams;

  private String bigramsFileName;

  private String statisticsFileName;

  private String temporaryIndex;

  private String webRoot;

  private String embeddingVectors;

  private String myStem;

  private String pathToZIP;

  private String pathToMetrics;

  private int pageSize = 10;


  @Override
  public Path getTemporaryDocuments() {
    return Paths.get(temporaryDocuments);
  }

  private void setTemporaryDocuments(String temporaryDocuments) {
    this.temporaryDocuments = temporaryDocuments;
  }

  @Override
  public String getTemporaryBigrams() {
    return temporaryBigrams;
  }

  private void setTemporaryBigrams(String temporaryBigrams) {
    this.temporaryBigrams = temporaryBigrams;
  }

  @Override
  public Path getBigramsFileName() {
    return getPathToZIP().toAbsolutePath().getParent().resolve(getTemporaryBigrams())
        .resolve(bigramsFileName);
  }

  private void setBigramsFileName(String bigramsFileName) {
    this.bigramsFileName = bigramsFileName;
  }

  @Override
  public Path getTemporaryIndex() {
    return getPathToZIP().toAbsolutePath().getParent().resolve(temporaryIndex);
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
  public String getStatisticsFileName() {
    return statisticsFileName;
  }

  private void setStatisticsFileName(String statisticsFileName) {
    this.statisticsFileName = statisticsFileName;
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