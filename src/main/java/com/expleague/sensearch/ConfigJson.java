package com.expleague.sensearch;

import java.nio.file.Path;

public interface ConfigJson {

  public Path getTemporaryDocuments();

  public String getTemporaryBigrams();

  public Path getBigramsFileName();

  public Path getTemporaryIndex();

  public String getWebRoot();

  public Path getMyStem();

  public Path getPathToZIP();

  public String getStatisticsFileName();

  public String getEmbeddingVectors();

  public Path getPathToMetrics();

  default boolean getBuildIndexFlag() {
    return false;
  }
}
