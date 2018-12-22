package com.expleague.sensearch.miner;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.miner.impl.AccumulatorFeatureSet;
import com.expleague.sensearch.miner.impl.QURLItem;
import com.expleague.sensearch.query.Query;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;


/**
 * Created by sandulmv on 02.11.18.
 */
public class MinerPhase implements SearchPhase {
  private static final Logger LOG = Logger.getLogger(MinerPhase.class.getName());

  private final Index index;
  private final AccumulatorFeatureSet features;
  private final int phaseId;

  @Inject
  public MinerPhase(Index index, @Assisted int phaseId) {
    this.index = index;
    this.features = new AccumulatorFeatureSet(index);
    this.phaseId = phaseId;
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.query() != null;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.info("Miner phase started");
    long startTime = System.nanoTime();

      final Map<Page, Features> documentsFeatures = new HashMap<>();
      Query query = whiteboard.query().get(phaseId);
      index.fetchDocuments(query).forEach(page -> {
        features.accept(new QURLItem(page, query));
        Vec all = features.advance();
        documentsFeatures.put(page, new Features() {
          @Override
          public Vec features() {
            return all;
          }

          @Override
          public Vec features(FeatureMeta... metas) {
            return new ArrayVec(
                Stream.of(metas).mapToInt(features::index).mapToDouble(all::get).toArray());
          }

          @Override
          public FeatureMeta meta(int index) {
            return features.meta(index);
          }

          @Override
          public int dim() {
            return features.dim();
          }
        });
      });

    whiteboard.putTextFeatures(documentsFeatures, phaseId);
    LOG.info(String.format("Miner phase finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }
}
