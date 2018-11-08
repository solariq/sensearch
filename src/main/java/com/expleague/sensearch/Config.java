package com.expleague.sensearch;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {

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


  public Path getTemporaryDocuments() {
    return Paths.get(temporaryDocuments);
  }

  private void setTemporaryDocuments(String temporaryDocuments) {
    this.temporaryDocuments = temporaryDocuments;
  }

  public String getTemporaryBigrams() {
    return temporaryBigrams;
  }

  private void setTemporaryBigrams(String temporaryBigrams) {
    this.temporaryBigrams = temporaryBigrams;
  }

  public Path getBigramsFileName() {
    return getPathToZIP().toAbsolutePath().getParent().resolve(getTemporaryBigrams())
        .resolve(bigramsFileName);
  }

  private void setBigramsFileName(String bigramsFileName) {
    this.bigramsFileName = bigramsFileName;
  }

  public Path getTemporaryIndex() {
    return getPathToZIP().toAbsolutePath().getParent().resolve(temporaryIndex);
  }

  private void setTemporaryIndex(String temporaryIndex) {
    this.temporaryIndex = temporaryIndex;
  }

  public String getWebRoot() {
    return webRoot;
  }

  private void setWebRoot(String webRoot) {
    this.webRoot = webRoot;
  }

  public Path getMyStem() {
    return Paths.get(myStem);
  }

  private void setMyStem(String myStem) {
    this.myStem = myStem;
  }

  public Path getPathToZIP() {
    return Paths.get(pathToZIP);
  }

  private void setPathToZIP(String pathToZIP) {
    this.pathToZIP = pathToZIP;
  }

  public String getStatisticsFileName() {
    return statisticsFileName;
  }

  private void setStatisticsFileName(String statisticsFileName) {
    this.statisticsFileName = statisticsFileName;
  }


  public String getEmbeddingVectors() {
    return embeddingVectors;
  }

  private void setEmbeddingVectors(String embeddingVectors) {
    this.embeddingVectors = embeddingVectors;
  }

  public Path getPathToMetrics() {
    return Paths.get(pathToMetrics);
  }

  private void setPathToMetrics(String pathToMetrics) {
    this.pathToMetrics = pathToMetrics;
  }
}