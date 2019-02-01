package com.expleague.sensearch;

import java.nio.file.Path;

public interface Config {

  Path getTemporaryDocuments();

  Path getTemporaryIndex();

  String getWebRoot();

  Path getMyStem();

  Path getPathToZIP();

  String getEmbeddingVectors();

  Path getPathToMetrics();

  int getPageSize();

  default boolean getBuildIndexFlag() {
    return false;
  }

  default boolean getTrainEmbeddingFlag() {
    return false;
  }

  default boolean getLshNearestFlag() {
    return true;
  }

  int maxFilterItems();
}
