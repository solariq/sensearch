package com.expleague.sensearch.metrics;

import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import com.google.inject.Inject;
import org.apache.log4j.Logger;

public class MetricPhase implements SearchPhase {

  private static final Logger LOG = Logger.getLogger(MetricPhase.class.getName());
  private final Metric metric;

  @Inject
  public MetricPhase(Metric metric) {
    this.metric = metric;
  }


  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.info("Metric phase started");
    long startTime = System.nanoTime();

    whiteboard.putGroundTruthResults(metric.calculate(whiteboard.input(), whiteboard.results()));

    LOG.info(
        String.format(
            "Metric phase finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.results() != null && whiteboard.input() != null;
  }
}
