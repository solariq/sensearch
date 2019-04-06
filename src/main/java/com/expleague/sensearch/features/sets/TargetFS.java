package com.expleague.sensearch.features.sets;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.ml.meta.TargetMeta;
import com.expleague.sensearch.features.QURLItem;

public class TargetFS extends FeatureSet.Stub<QURLItem>  implements TargetSet {

  private final static TargetMeta TARGET_META = TargetMeta
      .create("googleExists", "1 if Page exist at Google or 0 if not", ValueType.VEC);

  private double value;


  @Override
  public void acceptTargetValue(double value) {
    this.value = value;
  }

  @Override
  public Vec advance() {
    set(TARGET_META, value);
    return super.advance();
  }
}