package com.expleague.sensearch.ranking;

import com.expleague.commons.math.Trans;
import com.expleague.commons.util.Pair;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Annotations.PageSize;
import com.expleague.sensearch.core.Annotations.RankModel;
import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import com.expleague.sensearch.features.Features;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
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

  private final Trans model;
  private final FeatureMeta[] featuresInModel;

  @Inject
  public RankingPhase(@PageSize int pageSize, @Assisted int phaseId,
      @RankModel Pair<Function, FeatureMeta[]> rankModel) {
    this.pageSize = pageSize;
    this.phaseId = phaseId;
    this.model = (Trans) rankModel.getFirst();
    this.featuresInModel = rankModel.getSecond();
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.textFeatures().containsKey(phaseId);
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
            .collect(Collectors.toMap(Entry::getKey, p -> rank(p.getValue()))));

    whiteboard.putSubResult(
        Objects.requireNonNull(whiteboard
            .textFeatures())
            .get(phaseId)
            .entrySet()
            .stream()
            .map(p -> Pair.create(p.getKey(), rank(p.getValue())))
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

  private double rank(Features feat) {
    if (feat.isRequiredInResults()) {
      return Double.MAX_VALUE;
    }
    return model.trans(feat.features(featuresInModel)).get(0);
  }
}
