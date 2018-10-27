package components.embedding.impl;

import components.embedding.Filter;
import components.index.IndexedDocument;
import components.query.Query;

import java.util.stream.LongStream;
import java.util.stream.Stream;

public class FilterImpl implements Filter {

  private static final int numberOfNeighbors = 50;

  public FilterImpl(Stream<IndexedDocument> documentStream) {
    EmbeddingImpl.getInstance().setDocuments(documentStream);
  }

  @Override
  public LongStream filtrate(Query query) {
    return new NearestFinderImpl().getNearestDocuments(EmbeddingImpl.getInstance().getVec(query), numberOfNeighbors)
            .stream().mapToLong(l -> l);
  }
}