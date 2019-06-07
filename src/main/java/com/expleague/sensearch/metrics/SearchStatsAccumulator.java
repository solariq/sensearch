package com.expleague.sensearch.metrics;

import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.core.Whiteboard;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchStatsAccumulator {

  private double filterMinerRecall;
  private double filterRankRecall;
  private int queryNum;


  public void addQuery(Whiteboard whiteboard) {
    ResultItem[] groundTruth = whiteboard.groundTruthResults();
    Set<URI> groundTruthUris = Arrays.stream(Objects.requireNonNull(groundTruth))
        .map(ResultItem::reference)
        .collect(Collectors.toSet());

    queryNum += 1;

    final long rcMiner = whiteboard.filterFeatures().get(0).keySet().stream()
        .filter(page -> groundTruthUris.contains(page.uri()))
        .count();
    filterMinerRecall += 1.0 * rcMiner / groundTruth.length;

    final long rcRank = whiteboard.textFeatures().get(0).keySet().stream()
        .filter(page -> groundTruthUris.contains(page.uri()))
        .count();
    filterRankRecall += 1.0 * rcRank / groundTruth.length;
  }

  public EvaluationStats calcStats() {
    if (queryNum == 0) {
      throw new IllegalStateException("No queries to evaluate");
    }
    return new EvaluationStats(filterMinerRecall / queryNum, filterRankRecall / queryNum);
  }

  public static class EvaluationStats {

    private final double filterMinerRecall;
    private final double filterRankRecall;

    public EvaluationStats(double filterMinerRecall, double filterRankRecall) {
      this.filterMinerRecall = filterMinerRecall;
      this.filterRankRecall = filterRankRecall;
    }

    public double filterMinerRecall() {
      return filterMinerRecall;
    }

    public double filterRankRecall() {
      return filterRankRecall;
    }
  }
}
