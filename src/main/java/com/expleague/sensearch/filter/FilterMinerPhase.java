package com.expleague.sensearch.filter;

import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import com.expleague.sensearch.index.Index;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.log4j.Logger;

public class FilterMinerPhase implements SearchPhase {

  public static final int FILTERED_DOC_NUMBER = 5000;
  private static final Logger LOG = Logger.getLogger(Filter.class.getName());


  private final Index index;
  private final int phaseId;

  @Inject
  public FilterMinerPhase(Index index, @Assisted int phaseId) {
    this.index = index;
    this.phaseId = phaseId;
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.query().containsKey(phaseId);
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.info("FilterMiner phase started");
    long startTime = System.nanoTime();

    whiteboard.putFilterFeatures(index.fetchDocuments(whiteboard.query().get(phaseId), FILTERED_DOC_NUMBER), phaseId);

    LOG.info(
        String.format(
            "FilterMiner phase finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }



}
