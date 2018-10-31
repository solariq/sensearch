package com.expleague.sensearch.core;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.core.impl.WhiteboardImpl;
import com.expleague.sensearch.snippet.Segment;
import com.expleague.sensearch.snippet.Snippet;
import com.expleague.sensearch.web.Builder;

import java.net.URI;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SenSeArchImpl implements SenSeArch {
  private final Builder builder = new Builder();
  @Override
  public ResultPage search(String query, int pageNo) {
    final Set<? extends SearchPhase> phases = Stream.of(SearchPhase.FACTORIES).map(f -> {
      f.setConfig(builder);
      return f.get();
    }).collect(Collectors.toSet());



    final Whiteboard wb = new WhiteboardImpl(query, pageNo);
    final ThreadPoolExecutor executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() - 1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
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

    return new ResultPage() {
      @Override
      public int number() {
        return wb.pageNo();
      }

      @Override
      public int totalResultsFound() {
        return wb.totalResults();
      }

      @Override
      public ResultItem[] results() {
        final com.expleague.sensearch.Page[] pages = Objects.requireNonNull(wb.results());
        final Snippet[] snippets = wb.snippets();
        final ResultItem[] result = new ResultItem[snippets.length];
        for (int i = 0; i < result.length; i++) {
          int finalI = i;
          result[i] = new ResultItem() {
            @Override
            public URI reference() {
              return pages[finalI].reference();
            }

            @Override
            public CharSequence title() {
              return pages[finalI].title();
            }

            @Override
            public List<Pair<CharSequence, List<Segment>>> passages() {
              return Collections.singletonList(Pair.create(snippets[finalI].getContent(), snippets[finalI].getSelection()));
            }

            @Override
            public double score() {
              return 0;
            }
          };
        }
        return result;
      }
    };
  }
}
