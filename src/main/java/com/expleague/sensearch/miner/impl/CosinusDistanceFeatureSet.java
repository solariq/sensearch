package com.expleague.sensearch.miner.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import java.util.ArrayList;
import java.util.List;

public class CosinusDistanceFeatureSet extends FeatureSet.Stub<QURLItem> {

  public final static FeatureMeta COS_TITLE = FeatureMeta
      .create("cos-title", "cos distance between Query and Title", ValueType.VEC);
  public final static FeatureMeta COS_MIN_PASSAGE = FeatureMeta
      .create("cos-min-passage", "cos distance between Query and Body", ValueType.VEC);



  private Vec queryVec;
  private Vec titleVec;
  private List<Vec> passages = new ArrayList<>();

  @Override
  public void accept(QURLItem item) {
    passages.clear();
  }

  public void withPassage(Vec passage) {
    passages.add(passage);
  }

  public void withStats(Vec queryVec, Vec titleVec) {
    this.queryVec = queryVec;
    this.titleVec = titleVec;
  }

  @Override
  public Vec advance() {
    set(COS_TITLE, (1 - VecTools.cosine(queryVec, titleVec)) / 2);

    double maxCos = -1.0;
    for (Vec passage : passages) {
      maxCos = Math.max(maxCos, VecTools.cosine(queryVec, passage));
    }
    set(COS_MIN_PASSAGE, (1 - maxCos) / 2);
    return super.advance();
  }
}
