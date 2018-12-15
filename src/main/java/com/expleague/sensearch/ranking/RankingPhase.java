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

  private int rankerId;

  private static final Logger LOG = Logger.getLogger(RankingPhase.class.getName());

  private final int pageSize;
  private PointWiseRanker ranker;

  public RankingPhase(PointWiseRanker ranker, int pageSize) {
    this.ranker = ranker;
    this.pageSize = pageSize;
  }

  public RankingPhase(PointWiseRanker ranker, int pageSize, int rankerId) {
    this.ranker = ranker;
    this.pageSize = pageSize;
    this.rankerId = rankerId;
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.textFeatures() != null && whiteboard.textFeatures() != null && whiteboard.textFeatures()[rankerId] != null;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.info("Ranking phase started");
    long startTime = System.nanoTime();

    final int pageNo = whiteboard.pageNo();
    whiteboard.putResults(
        whiteboard
            .textFeatures()[rankerId]
            .entrySet()
            .stream()
            .map(p -> Pair.of(p.getKey(), p.getValue().features().get(0)))
            .sorted(Comparator.<Pair<Page, Double>>comparingDouble(Pair::getRight).reversed())
            .map(Pair::getLeft)
            .skip(pageNo * pageSize)
            .limit(pageSize)
            .toArray(Page[]::new));

    LOG.info(String
        .format("Ranking phase finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }
}
