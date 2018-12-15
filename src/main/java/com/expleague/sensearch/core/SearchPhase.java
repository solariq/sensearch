package com.expleague.sensearch.core;

import com.expleague.sensearch.metrics.MetricPhase;
import com.expleague.sensearch.miner.MinerPhase;
import com.expleague.sensearch.query.MergePhase;
import com.expleague.sensearch.query.QueryPhase;
import com.expleague.sensearch.ranking.RankingPhase;
import com.expleague.sensearch.snippet.SnippetPhase;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface SearchPhase extends Predicate<Whiteboard>, Consumer<Whiteboard> {

  @SuppressWarnings("unchecked")

  Class<? extends SearchPhase>[] SEARCH_PHASES =
      new Class[]{
          QueryPhase.class,
          SnippetPhase.class,
          RankingPhase.class,
          MinerPhase.class,
          MetricPhase.class
      };
}
