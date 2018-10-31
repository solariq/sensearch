package com.expleague.sensearch.core;

import com.expleague.sensearch.query.QueryPhase;
import com.expleague.sensearch.ranking.RankingPhase;
import com.expleague.sensearch.snippet.SnippetPhase;
import com.expleague.sensearch.web.Builder;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface SearchPhase extends Predicate<Whiteboard>, Consumer<Whiteboard> {
  @SuppressWarnings("unchecked")
  Factory<? extends SearchPhase>[] FACTORIES = new Factory[]{
      QueryPhase::new,
      SnippetPhase::new,
      new Factory<RankingPhase>() {
        Builder config;

        @Override
        public void setConfig(Builder builder) {
          this.config = builder;
        }

        @Override
        public RankingPhase get() {
          return new RankingPhase(config.getIndex(), config.windowSize(), config.pageSize());
        }
  }};

  interface Factory<T> extends Supplier<T> {
    default void setConfig(Builder builder) {}
  }
}
