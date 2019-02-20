package com.expleague.sensearch.snippet.features;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;

public class CosDistanceFeatureSet extends FeatureSet.Stub<QPASItem> {

  private final static FeatureMeta COS_DISTANCE = FeatureMeta
      .create("cos distance", "cos distance between query and passage", ValueType.VEC);

  private Vec query;
  private Vec passage;

  @Override
  public void accept(QPASItem item) {
    super.accept(item);
  }

  public void withVecs(Vec query, Vec passage) {
    this.query = query;
    this.passage = passage;
  }

  @Override
  public Vec advance() {
    set(COS_DISTANCE, (1 - VecTools.cosine(query, passage)) / 2.);
    return super.advance();
  }
}
