package com.expleague.sensearch.filter;

import com.expleague.commons.math.Trans;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.util.Pair;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Annotations.RankFilterModel;
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

public class FilterRankingPhase implements SearchPhase {


  private static final int FILTERED_DOC_NUMBER = 500;
  private static final Logger LOG = Logger.getLogger(Filter.class.getName());

  private final int phaseId;

  private final Trans model;
  private final FeatureMeta[] featuresInModel;

  @Inject
  public FilterRankingPhase(@Assisted int phaseId,
      @RankFilterModel Pair<Function, FeatureMeta[]> rankModel) {
    this.phaseId = phaseId;
    this.model = (Trans) rankModel.getFirst();
    this.featuresInModel = rankModel.getSecond();
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.filterFeatures() != null
        && Objects.requireNonNull(whiteboard.filterFeatures()).size() != 0
        && Objects.requireNonNull(whiteboard.filterFeatures()).get(phaseId) != null;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.info("FilterRanking phase started");
    long startTime = System.nanoTime();

    whiteboard.putPageFilterScores(
        Objects.requireNonNull(whiteboard
            .filterFeatures())
            .get(phaseId)
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Entry::getKey, p -> rank(p.getValue()))));

    whiteboard.putSubFilterResult(
        Objects.requireNonNull(whiteboard
            .filterFeatures())
            .get(phaseId)
            .entrySet()
            .stream()
            .map(p -> Pair.create(p.getKey(), rank(p.getValue())))
            .sorted(Comparator.<Pair<Page, Double>>comparingDouble(Pair::getSecond).reversed())
            .map(Pair::getFirst)
            .limit(FILTERED_DOC_NUMBER)
            .toArray(Page[]::new),
        phaseId);

    LOG.info(
        String.format(
            "FilterRanking phase finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }


  private double rank(Features feat) {
    if (feat.isRequiredInResults()) {
      return Double.MAX_VALUE;
    }
    return model.trans(feat.features(featuresInModel)).get(0);
  }

}
