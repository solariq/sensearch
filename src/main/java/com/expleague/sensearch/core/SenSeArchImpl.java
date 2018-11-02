package com.expleague.sensearch.core;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.core.impl.WhiteboardImpl;
import com.expleague.sensearch.snippet.Segment;
import com.expleague.sensearch.snippet.Snippet;
import com.expleague.sensearch.web.Builder;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
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
    final Set<? extends SearchPhase> phases = Stream.of(SearchPhase.FACTORIES).map(f -> {
      f.setConfig(builder);
      return f.get();
    }).collect(Collectors.toSet());



    final Whiteboard wb = new WhiteboardImpl(query, pageNo);
    final ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() - 1, Runtime.getRuntime().availableProcessors() - 1 , 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    while(wb.snippets() == null) {
      for (final SearchPhase phase : new ArrayList<>(phases)) {
        if (!phase.test(wb))
          continue;
        executor.execute(() -> {
          phase.accept(wb);
          synchronized (wb) {
            wb.notify();
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

    final com.expleague.sensearch.Page[] pages = Objects.requireNonNull(wb.results());
    final Snippet[] snippets = wb.snippets();
    final ResultItem[] result = new ResultItem[snippets.length];

    ResultItem[] results = new ResultItemImpl[snippets.length];
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
