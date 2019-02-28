package com.expleague.sensearch.miner;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.features.FeaturesImpl;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.query.Query;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.log4j.Logger;

/**
 * Created by sandulmv on 02.11.18.
 */
public class MinerPhase implements SearchPhase {

  private static final Logger LOG = Logger.getLogger(MinerPhase.class.getName());

  private final AccumulatorFeatureSet features;
  private final int phaseId;

  @Inject
  public MinerPhase(Index index, @Assisted int phaseId) {
    this.features = new AccumulatorFeatureSet(index);
    this.phaseId = phaseId;
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.query() != null &&
        whiteboard.subFilterResults().size() > phaseId &&
        whiteboard.subFilterResults().get(phaseId) != null &&
        Objects.requireNonNull(whiteboard.query()).size() >= phaseId &&
        Objects.requireNonNull(whiteboard.query()).get(phaseId) != null;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.info("Miner phase started");
    long startTime = System.nanoTime();

    final Map<Page, Features> documentsFeatures = new HashMap<>();
    Query query = Objects.requireNonNull(whiteboard.query()).get(phaseId);
    features.acceptFilterFeatures(whiteboard.filterFeatures().get(phaseId));
    System.out.println(whiteboard.subFilterResults().get(phaseId).length);
    Arrays.stream(whiteboard.subFilterResults().get(phaseId))
        .forEach(
            page -> {
              features.accept(new QURLItem(page, query));
              Vec all = features.advance();
              documentsFeatures.put(
                  page,
                  new FeaturesImpl(features, all));
            });

    whiteboard.putTextFeatures(documentsFeatures, phaseId);
    LOG.info(
        String.format(
            "Miner phase finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }
}
