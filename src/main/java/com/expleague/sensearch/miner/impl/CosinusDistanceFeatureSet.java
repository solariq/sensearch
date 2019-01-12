package com.expleague.sensearch.miner.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;

public class CosinusDistanceFeatureSet extends FeatureSet.Stub<QURLItem> {

  public final static FeatureMeta COSINUS_DISTANCE = FeatureMeta
      .create("cosinus distance", "cosinus distance between Query and Title", ValueType.VEC);


  private Vec queryVec;
  private Vec titleVec;

  public void withStats(Vec queryVec, Vec titleVec) {
    this.queryVec = queryVec;
    this.titleVec = titleVec;
  }

  @Override
  public Vec advance() {
    double result = 0.0;
    double querySum = 0.0;
    double titleSum = 0.0;

    for (int i = 0; i < queryVec.dim(); i++) {
      result += (queryVec.get(i) * titleVec.get(i));
      querySum += Math.pow(queryVec.get(i), 2);
      titleSum += Math.pow(titleVec.get(i), 2);
    }
    querySum = Math.sqrt(querySum);
    titleSum = Math.sqrt(titleSum);
    if ((querySum * titleSum) == 0.0) {
      result = 0;
    } else {
      result /= querySum * titleSum;
    }

    set(COSINUS_DISTANCE, result);
    return super.advance();
  }

}
