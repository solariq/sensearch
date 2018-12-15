package com.expleague.sensearch.core;

import com.expleague.sensearch.metrics.MetricPhase;
import com.expleague.sensearch.miner.MinerPhase;
import com.expleague.sensearch.query.QueryPhase;
import com.expleague.sensearch.ranking.RankingPhase;
import com.expleague.sensearch.snippet.SnippetPhase;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class SearchPhaseProvider {

  private final Provider<QueryPhase> queryPhaseProvider;
  private final Provider<SnippetPhase> snippetPhaseProvider;
  private final Provider<RankingPhase> rankingPhaseProvider;
  private final Provider<MinerPhase> minerPhaseProdider;
  private final Provider<MetricPhase> metricPhaseProvider;

  @Inject
  public SearchPhaseProvider(
      Provider<QueryPhase> queryPhaseProvider,
      Provider<SnippetPhase> snippetPhaseProvider,
      Provider<RankingPhase> rankingPhaseProvider,
      Provider<MinerPhase> minerPhaseProvider,
      Provider<MetricPhase> metricPhaseProvider) {
    this.queryPhaseProvider = queryPhaseProvider;
    this.snippetPhaseProvider = snippetPhaseProvider;
    this.rankingPhaseProvider = rankingPhaseProvider;
    this.minerPhaseProdider = minerPhaseProvider;
    this.metricPhaseProvider = metricPhaseProvider;
  }

  public SearchPhase get(Class<? extends SearchPhase> phase) {
    if (phase.equals(QueryPhase.class)) {
      return queryPhaseProvider.get();
    } else if (phase.equals(SnippetPhase.class)) {
      return snippetPhaseProvider.get();
    } else if (phase.equals(RankingPhase.class)) {
      return rankingPhaseProvider.get();
    } else if (phase.equals(MinerPhase.class)) {
      return minerPhaseProdider.get();
    } else if (phase.equals(MetricPhase.class)) {
      return metricPhaseProvider.get();
    } else {
      throw new IllegalArgumentException("Unknown phase " + phase.getName());
    }
  }
}
