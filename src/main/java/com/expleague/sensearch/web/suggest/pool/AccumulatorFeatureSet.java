package com.expleague.sensearch.web.suggest.pool;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;

public class AccumulatorFeatureSet extends FeatureSet.Stub<QSUGItem>{

  private final static FeatureMeta COS_DISTANCE = FeatureMeta.create("cos dist",
      "Cosine distance between qc and completing phrase", ValueType.VEC);
  
  private final static FeatureMeta TFIDF_COS_DISTANCE = FeatureMeta.create("tfidf cos dist",
      "Cosine distance between qc and completing phrase, tfidf weighted", ValueType.VEC);
  /*
  private final static FeatureMeta MALLET_COSINE = FeatureMeta.create("mallet vec cos",
      "Cosine distance by Mallet vectors", ValueType.VEC);
  */
  private final static FeatureMeta TF_IDF_SUM = FeatureMeta.create("tf idf sum",
      "tf idf sum of words", ValueType.VEC);
  
  private final static FeatureMeta VEC_LENGTH = FeatureMeta.create("phrase vec length",
      "Length of vector that represents phrase", ValueType.VEC);
  /*
  private final static FeatureMeta INTERSECT_SIZE = FeatureMeta.create("intersect",
      "number of intersection words", ValueType.VEC);
  */
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
    set(TFIDF_COS_DISTANCE, qSugItem.tfidfWeightedCosine);
    //set(MALLET_COSINE, qSugItem.malletCosine);
    set(TF_IDF_SUM, qSugItem.tfidfSum);
    set(VEC_LENGTH, qSugItem.vectorSumLength);
    set(INCOMING_LINKS, qSugItem.incomingLinksCount);
    //set(INTERSECT_SIZE, qSugItem.intersectionLength);
    set(PROB_COEF, qSugItem.probabilisticCoeff);
    
    return super.advance();
  }
}
