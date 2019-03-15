package com.expleague.sensearch.features;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.meta.FeatureMeta;

public class FeaturesForRequiredDocument implements Features {

  @Override
  public Vec features() {
    return null;
  }

  @Override
  public Vec features(FeatureMeta... metas) {
    return null;
  }

  @Override
  public FeatureMeta meta(int index) {
    return null;
  }

  @Override
  public int dim() {
    return 0;
  }

  @Override
  public boolean isRequiredInResults() {
    return true;
  }

}
