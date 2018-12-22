package com.expleague.sensearch.miner;

import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.miner.impl.RawTextFeaturesMiner;
import com.expleague.sensearch.query.Query;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;


/**
 * Created by sandulmv on 02.11.18.
 */
public class MinerPhase implements SearchPhase {

  private static final Logger LOG = Logger.getLogger(MinerPhase.class.getName());

  private final Index index;
  private final FeaturesMiner featuresExtractor;
  private final int phaseId;

  @Inject
  public MinerPhase(Index index, @Assisted int phaseId) {
    this.index = index;
    this.featuresExtractor = new RawTextFeaturesMiner(index);
    this.phaseId = phaseId;
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.query() != null && whiteboard.query().size() != 0 && whiteboard.query().get(phaseId) != null;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.info("Miner phase started");
    long startTime = System.nanoTime();

    final Query query = whiteboard.query().get(phaseId);
    whiteboard.putTextFeatures(
        index
            .fetchDocuments(query)
            .collect(
                Collectors.toMap(
                    Function.identity(), page -> featuresExtractor.extractFeatures(query, page))), phaseId);

    LOG.info(
        String.format(
            "Miner phase finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }
}
