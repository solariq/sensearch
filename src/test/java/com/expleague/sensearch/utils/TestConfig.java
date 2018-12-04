package com.expleague.sensearch.utils;

import com.expleague.sensearch.Config;
import java.nio.file.Path;

// TODO: more sensible config class with setters for all variables
public class TestConfig extends Config {

  private Path miniWikiPath;
  private Path indexRoot;
  private Path gloveVectorsPath;
  private Path tempDocumentsPath;

  // TODO: copy constructor!
  public TestConfig() {
  }

  public TestConfig(TestConfig other) {
    this.miniWikiPath = other.miniWikiPath;
    this.indexRoot = other.indexRoot;
    this.gloveVectorsPath = other.gloveVectorsPath;
    this.tempDocumentsPath = other.tempDocumentsPath;
  }

  @Override
  public Path getTemporaryDocuments() {
    if (tempDocumentsPath == null) {
      throw new UnsupportedOperationException("Path to temporary documents is not set!");
    }

    return tempDocumentsPath;
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
  public String getEmbeddingVectors() {
    return gloveVectorsPath.toString();
  }

  @Override
  public Path getPathToMetrics() {
    throw new UnsupportedOperationException();
  }

  public TestConfig setPathToZIP(Path pathToZIP) {
    this.miniWikiPath = pathToZIP;
    return this;
  }

  public TestConfig setEmbeddingVectors(Path gloveVectors) {
    this.gloveVectorsPath = gloveVectors;
    return this;
  }

  public TestConfig setTemporaryIndex(Path indexRoot) {
    this.indexRoot = indexRoot;
    return this;
  }

  public TestConfig setTemporaryDocuments(Path tempDocumentsPath) {
    this.tempDocumentsPath = tempDocumentsPath;
    return this;
  }
}
