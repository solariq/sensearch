package com.expleague.sensearch.ranking;

import com.expleague.commons.math.Trans;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Annotations.PageSize;
import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

;

/**
 * Point-wise ranker
 */
public class RankingPhase implements SearchPhase {

  private static final Logger LOG = Logger.getLogger(RankingPhase.class.getName());

  private final int pageSize;
  private final int phaseId;

  private static Trans model;

  static {
    try {
      model =
          DataTools.readModel(
              Objects.requireNonNull(
                  RankingPhase.class.getClassLoader().getResourceAsStream("models/ranking.model")));
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Inject
  public RankingPhase(@PageSize int pageSize, @Assisted int phaseId) {
    this.pageSize = pageSize;
    this.phaseId = phaseId;
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.textFeatures() != null
        && whiteboard.textFeatures().size() != 0
        && whiteboard.textFeatures().get(phaseId) != null;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.info("Ranking phase started");
    long startTime = System.nanoTime();

    final int pageNo = whiteboard.pageNo();

    // TODO: make it in one stream(?)
    whiteboard.putPageScores(
        whiteboard
            .textFeatures()
            .get(phaseId)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Entry::getKey, p -> rank(p.getValue().features()))));

    whiteboard.putSubResult(
        whiteboard
            .textFeatures()
            .get(phaseId)
            .entrySet()
            .stream()
            .map(p -> Pair.of(p.getKey(), rank(p.getValue().features())))
            .sorted(Comparator.<Pair<Page, Double>>comparingDouble(Pair::getRight).reversed())
            .map(Pair::getLeft)
            .skip(pageNo * pageSize)
            .limit(pageSize)
            .toArray(Page[]::new),
        phaseId);

    LOG.info(
        String.format(
            "Ranking phase finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }

  private double rank(Vec features) {
    /*double vec = 0;
    for (int ind = 0;  ind < features.dim(); ind++) {
      double normalize = 1.0 / (ind + 1);
      double f =features.get(ind);
      vec += (f * normalize);
    }
    return vec;*/
    return model.trans(features).get(0);
  }
}
