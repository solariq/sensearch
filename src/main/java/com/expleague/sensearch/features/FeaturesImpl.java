package com.expleague.sensearch.features;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.features.Features;
import java.util.stream.Stream;
import org.apache.log4j.Logger;

public class FeaturesImpl implements Features {

  private static final Logger LOG = Logger.getLogger(Features.class.getName());
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
            .map(meta -> {
              int res = features.index(meta);
              if (res == -1) {
                LOG.info(meta.description() + " not found!!!");
              }
              return res;
            })
            .filter(i -> i != -1)
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
