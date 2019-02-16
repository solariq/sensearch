package com.expleague.sensearch.miner;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import java.util.stream.Stream;

public class FeaturesImpl implements Features {

  private Vec all;
  private FeatureSet features;

  public FeaturesImpl(FeatureSet featureSet, Vec vec) {
    all = vec;
    features = featureSet;
  }

  @Override
  public Vec features() {
    return all;
  }

  @Override
  public Vec features(FeatureMeta... metas) {
    return new ArrayVec(
        Stream.of(metas)
          .mapToInt(features::index)
          .mapToDouble(all::get)
          .toArray());
  }

  @Override
  public FeatureMeta meta(int index) {
    return features.meta(index);
  }

  @Override
  public int dim() {
    return features.dim();
  }
}
