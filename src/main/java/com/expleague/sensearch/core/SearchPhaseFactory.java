package com.expleague.sensearch.core;

import com.expleague.sensearch.metrics.MetricPhase;
import com.expleague.sensearch.miner.MinerPhase;
import com.expleague.sensearch.query.QueryPhase;
import com.expleague.sensearch.ranking.RankingPhase;
import com.expleague.sensearch.snippet.SnippetPhase;

public interface SearchPhaseFactory {

  QueryPhase createQueryPhase();

  SnippetPhase createSnippetPhase(int id);

  RankingPhase createRankingPhase(int id);

  MinerPhase createMinerPhase(int id);

  MetricPhase createMetricPhase();
}
