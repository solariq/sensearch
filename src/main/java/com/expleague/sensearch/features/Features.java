package com.expleague.sensearch.features;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.meta.FeatureMeta;

/**
 * Created by sandulmv on 01.11.18.
 */
public interface Features {
  Vec features();
  Vec features(FeatureMeta... metas);

  FeatureMeta meta(int index);
  int dim();
  
  boolean isRequiredInResults();
}
