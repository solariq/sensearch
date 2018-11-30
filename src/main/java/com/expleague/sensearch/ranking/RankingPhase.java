package com.expleague.sensearch.ranking;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import java.util.Comparator;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

;

/**
 * Point-wise ranker
 */
public class RankingPhase implements SearchPhase {

  private static final Logger LOG = Logger.getLogger(RankingPhase.class.getName());

  private final int pageSize;
  private PointWiseRanker ranker;

  public RankingPhase(PointWiseRanker ranker, int pageSize) {
    this.ranker = ranker;
    this.pageSize = pageSize;
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.textFeatures() != null;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.debug("Ranking phase started");
    long startTime = System.nanoTime();

    final int pageNo = whiteboard.pageNo();
    whiteboard.putResults(
        whiteboard
            .textFeatures()
            .entrySet()
            .stream()
            .map(p -> Pair.of(p.getKey(), p.getValue().features().get(0)))
            .sorted(Comparator.<Pair<Page, Double>>comparingDouble(Pair::getRight).reversed())
            .map(Pair::getLeft)
            .skip(pageNo * pageSize)
            .limit(pageSize)
            .toArray(Page[]::new));

    LOG.debug(String
        .format("Ranking phase finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }
}
