package com.expleague.sensearch.core;

import com.expleague.sensearch.metrics.MetricPhase;
import com.expleague.sensearch.miner.MinerPhase;
import com.expleague.sensearch.query.MergePhase;
import com.expleague.sensearch.query.QueryPhase;
import com.expleague.sensearch.ranking.RankingPhase;
import com.expleague.sensearch.ranking.impl.Bm25Ranker;
import com.expleague.sensearch.snippet.SnippetPhase;
import com.expleague.sensearch.web.Builder;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface SearchPhase extends Predicate<Whiteboard>, Consumer<Whiteboard> {

  @SuppressWarnings("unchecked")
  Factory<? extends SearchPhase>[] FACTORIES =
      new Factory[] {
          QueryPhase::new,
          SnippetPhase::new,
          new Factory<RankingPhase>() {
            Builder builder;

            @Override
            public void setConfig(Builder builder) {
              this.builder = builder;
            }

            @Override
            public RankingPhase get() {
              return new RankingPhase(new Bm25Ranker(), builder.pageSize());
            }
          },
          new Factory<MinerPhase>() {
            Builder config;

            @Override
            public void setConfig(Builder builder) {
              this.config = builder;
            }

            @Override
            public MinerPhase get() {
              return new MinerPhase(config.getIndex());
            }
          },
          MergePhase::new,
          MetricPhase::new
      };

  interface Factory<T> extends Supplier<T> {

    default void setConfig(Builder builder) {
    }
  }
}
