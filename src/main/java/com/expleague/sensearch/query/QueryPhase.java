package com.expleague.sensearch.query;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import com.expleague.sensearch.index.Index;
import com.google.inject.Inject;
import org.apache.log4j.Logger;


public class QueryPhase implements SearchPhase {

  private static final Logger LOG = Logger.getLogger(QueryPhase.class.getName());
  private final Index index;

  @Inject
  public QueryPhase(Index index) {
    this.index = index;
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.input() != null;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.info("Query phase started");
    long startTime = System.nanoTime();

    final String input = whiteboard.input();

    IntObjectMap<Query> queries = new IntObjectHashMap<>();

    queries.put(0, BaseQuery.create(input, index));

    whiteboard.putQuery(queries);

    LOG.info(String
        .format("Query phase finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }
}
