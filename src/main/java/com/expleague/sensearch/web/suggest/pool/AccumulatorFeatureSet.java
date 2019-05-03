package com.expleague.sensearch.web.suggest.pool;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;

public class AccumulatorFeatureSet extends FeatureSet.Stub<QSUGItem>{

  private final static FeatureMeta COS_DISTANCE = FeatureMeta.create("cos dist",
      "Cosine distance between qc and completing phrase", ValueType.VEC);
  
  private final static FeatureMeta INTERSECT_SIZE = FeatureMeta.create("intersect",
      "number of intersection words", ValueType.VEC);
  
  private final static FeatureMeta INCOMING_LINKS = FeatureMeta.create("incoming_links",
      "Incoming links count", ValueType.VEC);
  
  private final static FeatureMeta PROB_COEF = FeatureMeta.create("probabilistic",
      "Coefficient from probabilistic model", ValueType.VEC);
  
  private QSUGItem qSugItem;
  
  
  @Override
  public void accept(QSUGItem item) {
    super.accept(item);
    
    this.qSugItem = item;
  }
  
  @Override
  public Vec advance() {
    set(COS_DISTANCE, qSugItem.cosine);
    set(INCOMING_LINKS, qSugItem.incomingLinksCount);
    set(INTERSECT_SIZE, qSugItem.intersectionLength);
    set(PROB_COEF, qSugItem.probabilisticCoeff);
    
    return super.advance();
  }
}
