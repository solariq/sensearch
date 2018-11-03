package com.expleague.sensearch.core.impl;

import com.expleague.sensearch.core.Embedding;
import com.expleague.sensearch.core.Filter;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.query.Query;

import java.util.stream.LongStream;
import java.util.stream.Stream;

public class FilterImpl implements Filter {

  private static final int numberOfNeighbors = 10;

  private Embedding embedding;

  public FilterImpl(Stream<IndexedPage> documentStream, Embedding embedding) {
    this.embedding = embedding;
    ((EmbeddingImpl) this.embedding).setDocuments(documentStream);
  }

  @Override
  public LongStream filtrate(Query query) {
    return embedding.getNearestDocuments(embedding.getVec(query), numberOfNeighbors)
            .stream().mapToLong(l -> l);
  }
}