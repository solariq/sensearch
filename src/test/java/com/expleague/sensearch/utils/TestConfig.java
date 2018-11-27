package com.expleague.sensearch.utils;


import com.expleague.sensearch.Config;
import java.nio.file.Path;

public class TestConfig extends Config {

  private Path miniWikiPath;
  private Path indexRoot;
  private Path gloveVectorsPath;

  public TestConfig(Path miniWikiPath, Path indexRoot, Path gloveVectorsPath) {
    this.miniWikiPath = miniWikiPath;
    this.indexRoot = indexRoot;
    this.gloveVectorsPath = gloveVectorsPath;
  }

  @Override
  public Path getTemporaryDocuments() {
    throw new UnsupportedOperationException();
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
}
