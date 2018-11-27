package com.expleague.sensearch.utils;


import com.expleague.sensearch.Config;
import java.nio.file.Path;

public class TestConfig extends Config {

  @Override
  public Path getTemporaryDocuments() {
    return super.getTemporaryDocuments();
  }

  @Override
  public String getTemporaryBigrams() {
    return super.getTemporaryBigrams();
  }

  @Override
  public Path getBigramsFileName() {
    return super.getBigramsFileName();
  }

  @Override
  public Path getTemporaryIndex() {
    return super.getTemporaryIndex();
  }

  @Override
  public String getWebRoot() {
    return super.getWebRoot();
  }

  @Override
  public Path getMyStem() {
    return super.getMyStem();
  }

  @Override
  public Path getPathToZIP() {
    return super.getPathToZIP();
  }

  @Override
  public String getStatisticsFileName() {
    return super.getStatisticsFileName();
  }

  @Override
  public String getEmbeddingVectors() {
    return super.getEmbeddingVectors();
  }

  @Override
  public Path getPathToMetrics() {
    return super.getPathToMetrics();
  }
}
