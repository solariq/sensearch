package com.expleague.sensearch.core.impl;

import com.expleague.sensearch.core.Filter;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.query.Query;

import java.util.stream.LongStream;
import java.util.stream.Stream;

public class FilterImpl implements Filter {

  private static final int numberOfNeighbors = 50;

  public FilterImpl(Stream<IndexedPage> documentStream) {
    EmbeddingImpl.getInstance().setDocuments(documentStream);
  }

  @Override
  public LongStream filtrate(Query query) {
    return new NearestFinderImpl().getNearestDocuments(EmbeddingImpl.getInstance().getVec(query), numberOfNeighbors)
            .stream().mapToLong(l -> l);
  }
}