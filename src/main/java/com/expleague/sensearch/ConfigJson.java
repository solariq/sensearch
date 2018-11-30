package com.expleague.sensearch;

import java.nio.file.Path;

public interface ConfigJson {

  public Path getTemporaryDocuments();

  public Path getTemporaryIndex();

  public String getWebRoot();

  public Path getMyStem();

  public Path getPathToZIP();

  public String getEmbeddingVectors();

  public Path getPathToMetrics();

  default boolean getBuildIndexFlag() {
    return false;
  }
}
