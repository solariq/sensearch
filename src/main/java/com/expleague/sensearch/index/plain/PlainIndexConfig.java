package com.expleague.sensearch.index.plain;

import com.google.gson.Gson;
import java.nio.file.Path;

public class PlainIndexConfig {
  private static final Gson GSON_CONVERTER = new Gson();

  private long version;
  private Path bigramsMapPath;
  private Path termFrequencyMapPath;
  private Path documentFrequencyMapPath;
  private Path embeddingVectorsPath;

  public PlainIndexConfig() {

  }

  public long version() {
    return version;
  }

  public Path bigramsMapPath() {
    return bigramsMapPath;
  }

  public Path termFrequencyMapPath() {
    return termFrequencyMapPath;
  }

  public Path documentFrequencyMapPath() {
    return documentFrequencyMapPath;
  }

  public Path embeddingVectorsPath() {
    return embeddingVectorsPath;
  }

  public static void createConfigFile(Path root,
      Path bigrasMapPath,
      Path termFrequencyMapPath,
      Path documentFrequencyMapPath,
      Path embeddingVectorsPath) {

  }

  private static PlainIndexConfig parseIndexConfig(Path root) {
    return null;
  }

}