package com.expleague.sensearch.miner.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;


public class BM25FeatureSet extends FeatureSet.Stub<QURLItem> {

  static FeatureMeta titleFeatureMeta = FeatureMeta.create("bm25title", "Title bm25", ValueType.VEC);

  @Override
  public void accept(QURLItem item) {

    // calc
  }

  @Override
  public Vec advance() {
//    return super.advance();
    set(titleFeatureMeta, 123);
    set(...);
    return super.advance();
  }
}
