package components.embedding.impl;

import components.embedding.Filter;
import components.query.Query;
import java.util.stream.Stream;

public class FilterImpl implements Filter {

  private static final int numberOfNeighbors = 50;

  @Override
  public Stream<Long> filtrate(Query query) {
    return new NearestFinderImpl()
        .getNearestDocuments(EmbeddingImpl.getInstance().getVec(query), numberOfNeighbors).stream();
  }
}
