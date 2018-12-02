package com.expleague.sensearch.query;

import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import org.apache.log4j.Logger;


public class QueryPhase implements SearchPhase {

  private static final Logger LOG = Logger.getLogger(QueryPhase.class.getName());

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.input() != null;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.info("Query phase started");
    long startTime = System.nanoTime();

    final String input = whiteboard.input();
    whiteboard.putQuery(BaseQuery.create(input, whiteboard.builder().getIndex()));

    LOG.info(String
        .format("Query phase finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }
}
