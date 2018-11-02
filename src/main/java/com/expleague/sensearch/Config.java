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

  private int pageSize = 10;


  public Path getTemporaryDocuments() {
    return Paths.get(temporaryDocuments);
  }

  public void setTemporaryDocuments(String temporaryDocuments) {
    this.temporaryDocuments = temporaryDocuments;
  }

  public String getTemporaryBigrams() {
    return temporaryBigrams;
  }

  public void setTemporaryBigrams(String temporaryBigrams) {
    this.temporaryBigrams = temporaryBigrams;
  }

  public Path getBigramsFileName() {
    return getPathToZIP().toAbsolutePath().getParent().resolve(getTemporaryBigrams())
        .resolve(bigramsFileName);
  }

  public void setBigramsFileName(String bigramsFileName) {
    this.bigramsFileName = bigramsFileName;
  }

  public Path getTemporaryIndex() {
    return getPathToZIP().toAbsolutePath().getParent().resolve(temporaryIndex);
  }

  public void setTemporaryIndex(String temporaryIndex) {
    this.temporaryIndex = temporaryIndex;
  }

  public String getWebRoot() {
    return webRoot;
  }

  public void setWebRoot(String webRoot) {
    this.webRoot = webRoot;
  }

  public String getMyStem() {
    return myStem;
  }

  public void setMyStem(String myStem) {
    this.myStem = myStem;
  }

  public Path getPathToZIP() {
    return Paths.get(pathToZIP);
  }

  public void setPathToZIP(String pathToZIP) {
    this.pathToZIP = pathToZIP;
  }

  public String getStatisticsFileName() {
    return statisticsFileName;
  }

  public void setStatisticsFileName(String statisticsFileName) {
    this.statisticsFileName = statisticsFileName;
  }


  public String getEmbeddingVectors() {
    return embeddingVectors;
  }

  public void setEmbeddingVectors(String embeddingVectors) {
    this.embeddingVectors = embeddingVectors;
  }

}