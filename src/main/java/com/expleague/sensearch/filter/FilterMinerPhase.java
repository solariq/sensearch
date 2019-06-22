package com.expleague.sensearch.filter;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Annotations.FilterMinerDocNum;
import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.features.FeaturesImpl;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.query.Query;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

public class FilterMinerPhase implements SearchPhase {

  private static final Logger LOG = Logger.getLogger(Filter.class.getName());


  private final Index index;
  private final int phaseId;
  private final int filterDocNum;

  @Inject
  public FilterMinerPhase(Index index, @FilterMinerDocNum int filterDocNum, @Assisted int phaseId) {
    this.index = index;
    this.phaseId = phaseId;
    this.filterDocNum = filterDocNum;
  }

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.query().containsKey(phaseId);
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    LOG.info("FilterMiner phase started");
    long startTime = System.nanoTime();

    AccumulatorFilterFeatureSet accumulatorFilterFs = new AccumulatorFilterFeatureSet(index);
    Query query = Objects.requireNonNull(whiteboard.query()).get(phaseId);

    Map<Page, Features> filterFeatures = index.fetchDocuments(query, filterDocNum).entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> {
          Page page = entry.getKey();
          Features pageFeatures = entry.getValue();
          accumulatorFilterFs.accept(new QURLItem(page, query));
          accumulatorFilterFs.withFilterDistFeatures(pageFeatures);
          Vec featureVec = accumulatorFilterFs.advance();
          return new FeaturesImpl(accumulatorFilterFs, featureVec);
        }));
    whiteboard.putFilterFeatures(filterFeatures, phaseId);

    LOG.info(
        String.format(
            "FilterMiner phase finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }


}
