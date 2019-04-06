package com.expleague.sensearch.features.sets;

import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.sensearch.features.QURLItem;

public interface TargetSet extends FeatureSet<QURLItem> {

  public void acceptTargetValue(double value);
}
