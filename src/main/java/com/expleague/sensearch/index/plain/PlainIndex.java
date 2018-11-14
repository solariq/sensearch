package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.index.embedding.Embedding;
import com.expleague.sensearch.index.embedding.Filter;
import com.expleague.sensearch.index.embedding.impl.FilterImpl;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.index.statistics.Stats;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import com.google.gson.Gson;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PlainIndex implements Index {

  public static class PlainIndexConfig {
    private static final Gson GSON_CONVERTER = new Gson();

    private long version;
    private Path bigramsMapPath;
    private Path termFrequencyMapPath;
    private Path documentFrequencyMapPath;
    private Path embeddingVectorsPath;

    private PlainIndexConfig() {

    }

    private long version() {
      return version;
    }

    private Path bigramsMapPath() {
      return bigramsMapPath;
    }

    private Path termFrequencyMapPath() {
      return termFrequencyMapPath;
    }

    private Path documentFrequencyMapPath() {
      return documentFrequencyMapPath;
    }

    private Path embeddingVectorsPath() {
      return embeddingVectorsPath;
    }

    public static void createConfigFile() {

    }

    private static PlainIndexConfig parseFromFile(Path configFile) {
      return null;
    }

  }

  private static TIntLongMap termFrequencyMap;
  private static TIntIntMap documentFrequencyMap;
  private static TObjectIntMap<String> wordToIdMap;
  private static double averagePageSize;
  private static int indexSize;
  private static int vocabularySize;
  // private static Embedding embedding;

  public PlainIndex() {
  }

  public static void initialize(Config config) throws Exception {

  }

  @Override
  public Stream<Page> fetchDocuments(Query query) {
    // there should be some call to embedding
    return null;
  }

  @Override
  public int indexSize() {
    return indexSize;
  }

  @Override
  public double averagePageSize() {
    return averagePageSize;
  }

  @Override
  public int documentFrequency(Term term) {
    return documentFrequencyMap.get(
        wordToIdMap.get(term.getRaw().toString().toLowerCase())
    );
  }

  @Override
  public long termFrequency(Term term) {
    return termFrequencyMap.get(
        wordToIdMap.get(term.getRaw().toString().toLowerCase())
    );
  }

  @Override
  public int vocabularySize() {
    return vocabularySize;
  }
}
