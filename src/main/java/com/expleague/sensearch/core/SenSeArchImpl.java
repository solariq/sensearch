package com.expleague.sensearch.core;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.core.Term.TermAndDistance;
import com.expleague.sensearch.core.impl.ResultItemDebugInfoImpl;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.core.impl.ResultPageImpl;
import com.expleague.sensearch.core.impl.SynonymAndScoreImpl;
import com.expleague.sensearch.core.impl.SynonymInfoImpl;
import com.expleague.sensearch.core.impl.WhiteboardImpl;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.snippet.Snippet;
import com.google.inject.Inject;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SenSeArchImpl implements SenSeArch {

  private final SearchPhaseFactory searchPhaseFactory;
  private final Index index;

  @Inject
  public SenSeArchImpl(SearchPhaseFactory searchPhaseFactory, Index index) {
    this.searchPhaseFactory = searchPhaseFactory;
    this.index = index;
  }

  @Override
  public ResultPage search(
      String query,
      int pageNo,
      boolean debug,
      boolean metric,
      List<? extends ResultItem> groundTruthData,
      Consumer<Whiteboard> debugInfoCollector) {
    final Whiteboard wb = new WhiteboardImpl(query, pageNo);

    final Set<SearchPhase> phases = new HashSet<>();
    phases.add(searchPhaseFactory.createQueryPhase());

    for (int i = 0; i < wb.queriesNumber(); ++i) {
      phases.add(searchPhaseFactory.createFilterMinerPhase(i));
      phases.add(searchPhaseFactory.createFilterRankingPhase(i));
      phases.add(searchPhaseFactory.createMinerPhase(i));
      phases.add(searchPhaseFactory.createRankingPhase(i));
      phases.add(searchPhaseFactory.createSnippetPhase(i));
    }

    if (metric) {
      if (groundTruthData == null || groundTruthData.isEmpty()) {
        phases.add(searchPhaseFactory.createMetricPhase());
      } else {
        wb.putGroundTruthResults(groundTruthData.toArray(new ResultItem[0]));
      }
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

    while (!hasError[0] && (wb.snippets() == null || (metric && wb.groundTruthResults() == null))) {
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
    executor.shutdown();

    if (hasError[0]) {
      throw new RuntimeException("Failed to process query", searchException[0]);
    }

    final Page[] pages = Objects.requireNonNull(wb.results());
    final Snippet[] snippets = wb.snippets();
    final ResultItem[] results = new ResultItem[snippets.length];

    List<Page> sortedRankedPages =
        Objects.requireNonNull(wb.pageScores())
            .entrySet()
            .stream()
            .sorted(Comparator.comparing(Entry::getValue))
            .map(Entry::getKey)
            .collect(Collectors.toList());
    List<Page> sortedFilteredPages =
        wb.pageFilterScores()
            .entrySet()
            .stream()
            .sorted(Comparator.comparing(Entry::getValue))
            .map(Entry::getKey)
            .collect(Collectors.toList());

    ResultItem[] debugDataResults = null;

    if (groundTruthData != null && debug) {
      debugDataResults = new ResultItem[groundTruthData.size()];
      for (int i = 0; i < groundTruthData.size(); i++) {
        ResultItem res = groundTruthData.get(i);

        Page page = index.page(res.reference());
        Features filterFeatures = wb.filterFeatures().get(0).get(page);
        if (filterFeatures == null) {
          filterFeatures = index.filterFeatures(BaseQuery.create(query, index), res.reference());
        }
        Features rankFeatures = wb.textFeatures().get(0).get(page);

        ResultItemDebugInfo debugInfo =
            new ResultItemDebugInfoImpl(
                res.reference().toString(),
                sortedRankedPages.indexOf(page),
                sortedFilteredPages.indexOf(page),
                wb.pageFilterScores().getOrDefault(page, -1.0),
                Objects.requireNonNull(wb.pageScores()).getOrDefault(page, -1.0),
                filterFeatures == null ? new double[0] : filterFeatures.features().toArray(),
                getMetaArray(filterFeatures),
                rankFeatures == null ? new double[0] : rankFeatures.features().toArray(),
                getMetaArray(rankFeatures));

        debugDataResults[i] =
            new ResultItemImpl(
                res.reference(), page.content(SegmentType.FULL_TITLE), res.passages(), debugInfo);
      }
    }

    for (int i = 0; i < snippets.length; i++) {
      Features rankFeatures = Objects.requireNonNull(wb.textFeatures()).get(0).get(pages[i]);
      Features filterFeatures = wb.filterFeatures().get(0).get(pages[i]);
      ResultItemDebugInfo debugInfo =
          debug
              ? new ResultItemDebugInfoImpl(
              pages[i].uri().toString(),
              i,
              sortedFilteredPages.indexOf(pages[i]),
              wb.pageFilterScores().get(pages[i]),
              Objects.requireNonNull(wb.pageScores()).get(pages[i]),
              filterFeatures.features().toArray(),
              getMetaArray(filterFeatures),
              rankFeatures.features().toArray(),
              getMetaArray(rankFeatures))
              : null;

      results[i] =
          new ResultItemImpl(
              pages[i].uri(),
              pages[i].content(SegmentType.SECTION_TITLE),
              Collections.singletonList(
                  Pair.create(snippets[i].getContent(), snippets[i].getSelection())),
              debugInfo);
    }

    if (debugInfoCollector != null) {
      debugInfoCollector.accept(wb);
    }
    return new ResultPageImpl(query, 0, snippets.length, results, debugDataResults);
  }

  private String[] getMetaArray(Features features) {
    if (features == null) {
      return new String[0];
    }
    return IntStream.range(0, features.dim())
        .mapToObj(id -> features.meta(id).id())
        .toArray(String[]::new);
  }

  @Override
  public List<SynonymInfo> synonyms(String uri, String query) {
    Page page = index.page(URI.create(uri));
    List<Term> content =
        index
            .parse(page.content(SegmentType.FULL_TITLE, SegmentType.BODY))
            .collect(Collectors.toList());
    List<Term> queryTerms = index.parse(query).collect(Collectors.toList());

    List<SynonymInfo> synonymInfos = new ArrayList<>();
    for (Term term : queryTerms) {
      Map<Term, Double> synonyms =
          term.synonymsWithDistance(BaseQuery.SYNONYM_THRESHOLD)
              .collect(
                  Collectors.toMap(
                      z -> z.term().lemma(),
                      TermAndDistance::distance,
                      (x, y) -> {
                        System.out.println(x + y);
                        return x;
                      }));
      SynonymAndScore[] synonymsInPage =
          content
              .stream()
              .filter(contentTerm -> synonyms.containsKey(contentTerm.lemma()))
              .distinct()
              .map(z -> new SynonymAndScoreImpl(synonyms.get(z.lemma()), z.text().toString()))
              .toArray(SynonymAndScore[]::new);
      synonymInfos.add(new SynonymInfoImpl(term.text().toString(), synonymsInPage));
    }

    return synonymInfos;
  }
}
