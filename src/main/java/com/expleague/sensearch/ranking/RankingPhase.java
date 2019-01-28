package com.expleague.sensearch.ranking;

import com.expleague.commons.math.Trans;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.util.Pair;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Annotations.PageSize;
import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

/**
 * Point-wise ranker
 */
public class RankingPhase implements SearchPhase {

  private static final Logger LOG = Logger.getLogger(RankingPhase.class.getName());

  private final int pageSize;
  private final int phaseId;

  private static final Trans model;
  private static final FeatureMeta[] featuresInModel;

  static {
    Pair<Function, FeatureMeta[]> pair = DataTools.readModel(
        new InputStreamReader(Objects.requireNonNull(
            RankingPhase.class.getClassLoader().getResourceAsStream("models/ranking.model")
        ), StandardCharsets.UTF_8)
    );
    model = (Trans)pair.getFirst();
    featuresInModel = pair.getSecond();
  }

  @Inject
  public RankingPhase(@PageSize int pageSize, @Assisted int phaseId) {
    this.pageSize = pageSize;
    this.phaseId = phaseId;
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.textFeatures() != null
        && Objects.requireNonNull(whiteboard.textFeatures()).size() != 0
        && Objects.requireNonNull(whiteboard.textFeatures()).get(phaseId) != null;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.info("Ranking phase started");
    long startTime = System.nanoTime();

    final int pageNo = whiteboard.pageNo();

    // TODO: make it in one stream(?)
    whiteboard.putPageScores(
        Objects.requireNonNull(whiteboard
            .textFeatures())
            .get(phaseId)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Entry::getKey, p -> rank(p.getValue().features(featuresInModel)))));

    whiteboard.putSubResult(
        Objects.requireNonNull(whiteboard
            .textFeatures())
            .get(phaseId)
            .entrySet()
            .stream()
            .map(p -> Pair.create(p.getKey(), rank(p.getValue().features(featuresInModel))))
            .sorted(Comparator.<Pair<Page, Double>>comparingDouble(Pair::getSecond).reversed())
            .map(Pair::getFirst)
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
