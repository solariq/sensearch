package com.expleague.sensearch.miner.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;

public class CosinusDistanceFeatureSet extends FeatureSet.Stub<QURLItem> {
  public final static FeatureMeta COSINUS_DISTANCE = FeatureMeta
      .create("cos-title", "cosinus distance between Query and Title", ValueType.VEC);

  private Vec queryVec;
  private Vec titleVec;

  public void withStats(Vec queryVec, Vec titleVec) {
    this.queryVec = queryVec;
    this.titleVec = titleVec;
  }

  @Override
  public Vec advance() {
    set(COSINUS_DISTANCE, 1 - 2 * VecTools.cosine(queryVec, titleVec));
    return super.advance();
  }
}
