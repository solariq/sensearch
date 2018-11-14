package com.expleague.sensearch.core;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.core.impl.WhiteboardImpl;
import com.expleague.sensearch.snippet.Segment;
import com.expleague.sensearch.snippet.Snippet;
import com.expleague.sensearch.web.Builder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SenSeArchImpl implements SenSeArch {
  private final Builder builder;

  public SenSeArchImpl(Builder builder){
    this.builder = builder;
  }

  @Override
  public ResultPage search(String query, int pageNo) {
    final Set<SearchPhase> phases = Stream.of(SearchPhase.FACTORIES).map(f -> {
      f.setConfig(builder);
      return f.get();
    }).collect(Collectors.toSet());



    final Whiteboard wb = new WhiteboardImpl(query, pageNo, builder);
    final ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() - 1, Runtime.getRuntime().availableProcessors() - 1 , 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    boolean[] hasError = new boolean[1];

    while(!hasError[0] && wb.snippets() == null) {
      List<SearchPhase> curPhases = new ArrayList<>(phases);
      phases.clear();
      for (final SearchPhase phase : curPhases) {
        if (!phase.test(wb)) {
          phases.add(phase);
          continue;
        }
        executor.execute(() -> {
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
        }
        catch (InterruptedException ignore) { }
      }
    }

    if (hasError[0]) {
      throw new RuntimeException("Failed to process query");
    }

    final com.expleague.sensearch.Page[] pages = Objects.requireNonNull(wb.results());
    final Snippet[] snippets = wb.snippets();
    final ResultItem[] results = new ResultItem[snippets.length];

    for (int i = 0; i < snippets.length; i++) {
      results[i] = new ResultItemImpl(
          pages[i].reference(),
          pages[i].title(),
          Arrays.asList(Pair.create(snippets[i].getContent(), snippets[i].getSelection())),
          0);
    }

    return new ResultPageImpl(0, snippets.length, results);
  }

  public class ResultItemImpl implements ResultItem {
    private final URI reference;
    private final CharSequence title;
    private final List<Pair<CharSequence, List<Segment>>> passages;
    private final double score;

    ResultItemImpl(
        URI reference,
        CharSequence title,
        List<Pair<CharSequence, List<Segment>>> passages,
        double score) {
      this.reference = reference;
      this.title = title;
      this.passages = passages;
      this.score = score;
    }


    @Override
    public URI reference() {
      return reference;
    }

    @Override
    public CharSequence title() {
      return title;
    }

    @Override
    public List<Pair<CharSequence, List<Segment>>> passages() {
      return passages;
    }

    @Override
    public double score() {
      return score;
    }
  }

  public class ResultPageImpl implements ResultPage {

    private final int number;
    private final int totalResults;
    private final ResultItem[] results;

    public ResultPageImpl(int number, int totalResults, ResultItem[] results) {
      this.number = number;
      this.totalResults = totalResults;
      this.results = results;
    }

    @Override
    public int number() {
      return number;
    }

    @Override
    public int totalResultsFound() {
      return totalResults;
    }

    @Override
    public ResultItem[] results() {
      return results;
    }
  }
}
