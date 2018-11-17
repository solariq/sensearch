package com.expleague.sensearch.metrics;

import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;

public class MetricPhase implements SearchPhase {

  //TODO implement as Phase and delete in SenSeArchImpl and "(ResultItem[])"
  @Override
  public void accept(Whiteboard whiteboard) {
    Metric metric = whiteboard.builder().metric();
    whiteboard.putGoogleResults(metric.calculate(whiteboard.input(), whiteboard.results()));
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.results() != null && whiteboard.input() != null;
  }
}
