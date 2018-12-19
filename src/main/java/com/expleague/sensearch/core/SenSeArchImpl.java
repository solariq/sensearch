package com.expleague.sensearch.core;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.core.impl.ResultPageImpl;
import com.expleague.sensearch.core.impl.WhiteboardImpl;
import com.expleague.sensearch.snippet.Snippet;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SenSeArchImpl implements SenSeArch {

  private final SearchPhaseFactory searchPhaseFactory;

  @Inject
  public SenSeArchImpl(SearchPhaseFactory searchPhaseFactory) {
    this.searchPhaseFactory = searchPhaseFactory;
  }

  @Override
  public ResultPage search(String query, int pageNo) {
    final Set<SearchPhase> phases = new HashSet<>();
    phases.add(searchPhaseFactory.createQueryPhase());

    phases.add(searchPhaseFactory.createMinerPhase(0));
    phases.add(searchPhaseFactory.createRankingPhase(0));
    phases.add(searchPhaseFactory.createSnippetPhase(0));

    phases.add(searchPhaseFactory.createMetricPhase());

    final Whiteboard wb = new WhiteboardImpl(query, pageNo);
    final ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() - 1,
            Runtime.getRuntime().availableProcessors() - 1,
            1,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());

    boolean[] hasError = new boolean[1];

    while (!hasError[0] && (wb.snippets() == null || wb.googleResults() == null)) {
      List<SearchPhase> curPhases = new ArrayList<>(phases);
      phases.clear();
      for (final SearchPhase phase : curPhases) {
        if (!phase.test(wb)) {
          phases.add(phase);
          continue;
        }
        executor.execute(
            () -> {
              try {
                phase.accept(wb);
                synchronized (wb) {
                  wb.notify();
                }
              } catch (Exception e) {
                synchronized (wb) {
                  e.printStackTrace();
                  hasError[0] = true;
                  wb.notify();
                }
              }
            });
      }
      synchronized (wb) {
        try {
          wb.wait();
        } catch (InterruptedException ignore) {
        }
      }
    }

    if (hasError[0]) {
      throw new RuntimeException("Failed to process query");
    }

    final com.expleague.sensearch.Page[] pages = Objects.requireNonNull(wb.results());
    final Snippet[] snippets = wb.snippets();
    final ResultItem[] results = new ResultItem[snippets.length];
    final ResultItem[] googleResults = wb.googleResults();
    for (int i = 0; i < snippets.length; i++) {
      results[i] =
          new ResultItemImpl(
              pages[i].uri(),
              pages[i].title(),
              Arrays.asList(Pair.create(snippets[i].getContent(), snippets[i].getSelection())),
              0);
    }

    return new ResultPageImpl(0, snippets.length, results, googleResults);
  }
}
