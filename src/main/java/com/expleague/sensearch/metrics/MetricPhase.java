package com.expleague.sensearch.metrics;

import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import org.apache.log4j.Logger;

public class MetricPhase implements SearchPhase {

  private static final Logger LOG = Logger.getLogger(MetricPhase.class.getName());

  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.debug("Metric phase started");
    long startTime = System.nanoTime();

    Metric metric = whiteboard.builder().metric();
    whiteboard.putGoogleResults(metric.calculate(whiteboard.input(), whiteboard.results()));

    LOG.debug(
        String.format(
            "Metric phase finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.results() != null && whiteboard.input() != null;
  }
}
