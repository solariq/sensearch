package com.expleague.sensearch.core;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.core.impl.ResultItemDebugInfoImpl;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.core.impl.ResultPageImpl;
import com.expleague.sensearch.core.impl.WhiteboardImpl;
import com.expleague.sensearch.miner.Features;
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
import java.util.stream.IntStream;

public class SenSeArchImpl implements SenSeArch {

  private final SearchPhaseFactory searchPhaseFactory;

  @Inject
  public SenSeArchImpl(SearchPhaseFactory searchPhaseFactory) {
    this.searchPhaseFactory = searchPhaseFactory;
  }

  @Override
  public ResultPage search(String query, int pageNo, boolean debug, boolean metric) {
    final Whiteboard wb = new WhiteboardImpl(query, pageNo);

    final Set<SearchPhase> phases = new HashSet<>();
    phases.add(searchPhaseFactory.createQueryPhase());

    for (int i = 0; i < wb.queriesNumber(); ++i) {
      phases.add(searchPhaseFactory.createMinerPhase(i));
      phases.add(searchPhaseFactory.createRankingPhase(i));
      phases.add(searchPhaseFactory.createSnippetPhase(i));
    }

    if (metric) {
      phases.add(searchPhaseFactory.createMetricPhase());
    }
    phases.add(searchPhaseFactory.createMergePhase());

    final ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() - 1,
            Runtime.getRuntime().availableProcessors() - 1,
            1,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>());

    boolean[] hasError = new boolean[1];
    Exception[] searchException = new Exception[1];

    while (!hasError[0] && (wb.snippets() == null || (metric && wb.googleResults() == null))) {
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
                  searchException[0] = e;
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
      throw new RuntimeException("Failed to process query", searchException[0]);
    }

    final com.expleague.sensearch.Page[] pages = Objects.requireNonNull(wb.results());
    final Snippet[] snippets = wb.snippets();
    final ResultItem[] results = new ResultItem[snippets.length];
    final ResultItem[] googleResults = wb.googleResults();

    if (googleResults != null && debug) {
      for (int i = 0; i < googleResults.length; i++) {
        ResultItem res = googleResults[i];

        Page page =
            wb.pageScores()
                .keySet()
                .stream()
                .filter(p -> p.content(SegmentType.SECTION_TITLE).equals(res.title()))
                .findFirst()
                .orElse(null);
        googleResults[i] =
            new ResultItemImpl(
                res.reference(),
                res.title(),
                res.passages(),
                page == null ? -1 : wb.pageScores().get(page),
                null);
      }
    }

    for (int i = 0; i < snippets.length; i++) {
      Features features = wb.textFeatures().get(0).get(pages[i]);
      ResultItemDebugInfo debugInfo =
          debug
              ? new ResultItemDebugInfoImpl(
              i,
              features.features().toArray(),
              IntStream.range(0, features.dim())
                  .mapToObj(id -> features.meta(id).id())
                  .toArray(String[]::new))
              : null;

      results[i] =
          new ResultItemImpl(
              pages[i].uri(),
              pages[i].content(SegmentType.SECTION_TITLE),
              Arrays.asList(Pair.create(snippets[i].getContent(), snippets[i].getSelection())),
              wb.pageScores().get(pages[i]),
              debugInfo);
    }

    return new ResultPageImpl(0, snippets.length, results, googleResults);
  }
}
