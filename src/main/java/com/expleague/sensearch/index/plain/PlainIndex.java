package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.embedding.Embedding;
import com.expleague.sensearch.index.embedding.Filter;
import com.expleague.sensearch.index.embedding.impl.EmbeddingImpl;
import com.expleague.sensearch.index.embedding.impl.FilterImpl;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TObjectIntMap;
import java.util.stream.Stream;

public class PlainIndex implements Index {

  private static final int DOC_NUMBER = 50;
  private static final int SYN_NUMBER = 50;
  private static final Embedding embedding = new EmbeddingImpl(/*smth*/null);
  private static final Filter filter = new FilterImpl(/*smth*/null);
  private static TIntLongMap termFrequencyMap;
  private static TIntIntMap documentFrequencyMap;
  private static TObjectIntMap<String> wordToIdMap;
  private static double averagePageSize;
  private static int indexSize;
  private static int vocabularySize;

  public PlainIndex() {
  }

  @Override
  public Term[] synonyms(Term term) {
    return filter
        .filtrate(
            embedding.getVec(wordToIdMap.get(term.getRaw().toString().toLowerCase())),
            SYN_NUMBER,
            this::isWord
        )
        .mapToObj(this::getWordById)
        .toArray(Term[]::new);
  }

  //todo implement
  private Page getPageById(long id) {
    return null;
  }

  //todo implement
  private String getWordById(long id) {
    return null;
  }

  public Stream<CharSequence> allTitles() {
    return null;
  }

  @Override
  public Stream<Page> fetchDocuments(Query query) {
    return filter.filtrate(
        //todo replace with smart tokenizer for terms
        embedding.getVec(
            query.getTerms().stream().mapToLong(t -> wordToIdMap.get(t.getRaw().toString()))),
        DOC_NUMBER,
        this::isPage
    ).mapToObj(this::getPageById);
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
