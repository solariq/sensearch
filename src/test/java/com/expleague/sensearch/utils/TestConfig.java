package com.expleague.sensearch.utils;

import com.expleague.sensearch.Config;
import java.nio.file.Path;

public class TestConfig extends Config {

  private Path miniWikiPath;
  private Path indexRoot;
  private Path gloveVectorsPath;
  private Path tempDocumentsPath;

  // TODO: copy constructor!
  TestConfig(Path miniWikiPath, Path gloveVectorsPath) {
    this.miniWikiPath = miniWikiPath;
    this.gloveVectorsPath = gloveVectorsPath;
  }

  @Override
  public Path getTemporaryDocuments() {
    if (tempDocumentsPath == null) {
      throw new UnsupportedOperationException("Path to temporary documents is not set!");
    }

    return tempDocumentsPath;
  }

  @Override
  public String getTemporaryBigrams() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path getBigramsFileName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path getTemporaryIndex() {
    if (indexRoot == null) {
      throw new UnsupportedOperationException("Path to index root is not set!");
    }

    return indexRoot;
  }

  @Override
  public String getWebRoot() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path getMyStem() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Path getPathToZIP() {
    return miniWikiPath;
  }

  @Override
  public String getStatisticsFileName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getEmbeddingVectors() {
    return gloveVectorsPath.toString();
  }

  @Override
  public Path getPathToMetrics() {
    throw new UnsupportedOperationException();
  }

  public void setIndexRoot(Path indexRoot) {
    this.indexRoot = indexRoot;
  }

  public void setTempDocumentsPath(Path tempDocumentsPath) {
    this.tempDocumentsPath = tempDocumentsPath;
  }
}
