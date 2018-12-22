package com.expleague.sensearch.query;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;

public class MergePhase implements SearchPhase {

  private static final Logger LOG = Logger.getLogger(MergePhase.class.getName());

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.subResults() != null && allSubResultsArePresent(whiteboard);
  }

  private boolean allSubResultsArePresent(Whiteboard whiteboard) {

    if (whiteboard.subResults() == null) {
      return false;
    }

    boolean result = true;

    for (Page[] subResult : whiteboard.subResults()) {
      if (subResult == null) {
        result = false;
      }
    }

    return result;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.info("Merge phase started");
    long startTime = System.nanoTime();

    final List<Page> results = new ArrayList<>();

    for (Page[] subResult : whiteboard.subResults()) {
      Collections.addAll(results, subResult);
    }

    whiteboard.putResults(results.toArray(new Page[0]));

    LOG.info(String
        .format("Merge phase finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }
}
