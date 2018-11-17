package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Embedding;
import com.expleague.sensearch.core.Filter;;
import com.expleague.sensearch.index.embedding.impl.EmbeddingImpl;
import com.expleague.sensearch.index.embedding.impl.FilterImpl;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
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

//todo сделать собсна
public class PlainIndex implements Index {

  private static final int DOC_NUMBER = 50;
  private static final int SYN_NUMBER = 50;

  private static TIntLongMap termFrequencyMap;
  private static TIntIntMap documentFrequencyMap;
  private static TObjectIntMap<String> wordToIdMap;
  private static double averagePageSize;
  private static int indexSize;
  private static int vocabularySize;
  private static final Embedding embedding = new EmbeddingImpl(/*smth*/null);
  private static final Filter filter = new FilterImpl(/*smth*/null);

  public PlainIndex() {
  }

  public static void initialize(Config config) throws Exception {
    // parse paths and maps
  }

  //todo implement
  private Page getPageById(long id) {
    return null;
  }

  //todo implement
  private String getWordById(long id) {
    return null;
  }

  @Override
  public Stream<Page> fetchDocuments(Query query) {
    return filter.filtrate(
            //todo replace with smart tokenizer for terms
            embedding.getVec(query.getTerms().stream().mapToLong(t -> wordToIdMap.get(t.getRaw().toString()))),
            DOC_NUMBER,
            this::isPage
    ).mapToObj(this::getPageById);
  }

  @Override
  public Stream<String> getSynonyms(String word) {
    return filter.filtrate(
            embedding.getVec(wordToIdMap.get(word)),
            SYN_NUMBER,
            this::isWord
    ).mapToObj(this::getWordById);
  }

  @Override
  public boolean isPage(long id) {
    return id >= 0;
  }

  @Override
  public boolean isWord(long id) {
    return id < 0;
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
