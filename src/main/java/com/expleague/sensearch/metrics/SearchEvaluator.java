package com.expleague.sensearch.metrics;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.metrics.SearchStatsAccumulator.EvaluationStats;
import com.expleague.sensearch.miner.pool.QueryAndResults;
import com.expleague.sensearch.miner.pool.QueryAndResults.PageAndWeight;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SearchEvaluator {

  private final ConfigImpl searchConfig;
  private final int startRecallEvalNum;
  private final int endRecallEvalNum;
  private final double recallEvalStep;

  public SearchEvaluator(
      ConfigImpl searchConfig,
      int startRecallEvalNum,
      int endRecallEvalNum,
      double recallEvalStep) {
    this.searchConfig = searchConfig;
    this.startRecallEvalNum = startRecallEvalNum;
    this.endRecallEvalNum = endRecallEvalNum;
    this.recallEvalStep = recallEvalStep;
  }

  public void evalAndPrint(QueryAndResults[] queryData) {
    List<Integer> grid = new ArrayList<>();
    {
      int cur = startRecallEvalNum;
      while (cur <= endRecallEvalNum) {
        grid.add(cur);
        cur *= recallEvalStep;
      }
    }

    double[] minerGridRecall = new double[grid.size()];
    double[][] rankMinerGridRecall = new double[grid.size()][grid.size()];

    Injector injector = Guice.createInjector(new AppModule(searchConfig));
    SenSeArch search = injector.getInstance(SenSeArch.class);

    for (int i = 0; i < grid.size(); i++) {
      for (int j = 0; j <= i; j++) {
        final int filterMinerNum = grid.get(i);
        final int filterRankNum = grid.get(j);

        searchConfig.setFilterMinerDocNum(filterMinerNum);
        searchConfig.setFilterRankDocNum(filterRankNum);

        SearchStatsAccumulator searchStatsAccumulator = new SearchStatsAccumulator();
        for (QueryAndResults queryElem : queryData) {
          if (queryElem.getAnswers().length == 0) {
            continue;
          }
          List<ResultItemImpl> groundTruth = Arrays.stream(queryElem.getAnswers())
              .map(PageAndWeight::getUri)
              .map(uri -> new ResultItemImpl(uri, "", Collections.emptyList(), null))
              .collect(Collectors.toList());
          search.search(queryElem.getQuery(), 0, false, true, groundTruth, searchStatsAccumulator::addQuery);
        }

        EvaluationStats evaluationStats = searchStatsAccumulator.calcStats();

        minerGridRecall[i] = evaluationStats.filterMinerRecall();
        rankMinerGridRecall[j][i] = evaluationStats.filterRankRecall();
      }

    }

    printResults(grid, minerGridRecall, rankMinerGridRecall);

  }

  private void printResults(List<Integer> grid, double[] minerGridRecall, double[][] rankMinerGridRecall) {
    System.out.println("Filter miner recall:");
    for (int i = 0; i < grid.size(); i++) {
      System.out.println(String.format("%-10s %.4f", prettyRecallNum(grid.get(i)), minerGridRecall[i]));
    }
    System.out.println("\n");
    System.out.println("Recall for miner & rank");

    System.out.print("+-------+");
    for (int i = 0; i < grid.size(); i++) {
      System.out.print("-------+");
    }
    System.out.println();

    System.out.print("|      M|");
    for (int i = 0; i < grid.size(); i++) {
      System.out.print("       |");
    }
    System.out.println();
    System.out.print("| R     |");

    grid.forEach(cnt -> System.out.print(String.format(" %-5s |", prettyRecallNum(cnt))));
    System.out.println();

    System.out.print("+-------+");
    for (int i = 0; i < grid.size(); i++) {
      System.out.print("-------+");
    }
    System.out.println();

    for (int i = 0; i < rankMinerGridRecall.length; i++) {
      final String rankNum = prettyRecallNum(grid.get(i));
      System.out.print(String.format("| %-5s |", rankNum));

      for (int j = 0; j < rankMinerGridRecall[i].length; j++) {
        System.out.print(String.format(" %.3f |", rankMinerGridRecall[i][j]));
      }
      System.out.println();

      System.out.print("+-------+");
      for (int j = 0; j < grid.size(); j++) {
        System.out.print("-------+");
      }
      System.out.println();
    }
  }


  private String prettyRecallNum(int num) {
    if (num < 100) {
      return Integer.toString(num);
    }
    if (num < 100_000) {
      return num / 1000.0 + "K";
    }
    if (num < 100_000_000) {
      return num / 1000_000.0 + "M";
    }
    return num / 1e9 + "G";
  }
}
