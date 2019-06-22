package com.expleague.sensearch.filter;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.features.sets.filter.FilterDistFeatureSet;
import com.expleague.sensearch.features.sets.filter.QueryFeatureSet;
import com.expleague.sensearch.index.Index;

public class AccumulatorFilterFeatureSet extends FeatureSet.Stub<QURLItem> {

  private final FilterDistFeatureSet filterDistFs = new FilterDistFeatureSet();
  private final QueryFeatureSet queryFs = new QueryFeatureSet();

  private final FeatureSet<QURLItem> features = FeatureSet.join(
      filterDistFs,
      queryFs
  );

  private final Index index;

  public AccumulatorFilterFeatureSet(Index index) {
    this.index = index;
    queryFs.withIndex(index);
  }

  @Override
  public void accept(QURLItem item) {
    super.accept(item);
    features.accept(item);
  }

  public void withFilterDistFeatures(Features features) {
    filterDistFs.withBody(features.features(FilterDistFeatureSet.SECTION).get(0));
    filterDistFs.withLink(features.features(FilterDistFeatureSet.LINK).get(0));
    filterDistFs.withTitle(features.features(FilterDistFeatureSet.TITLE).get(0));
  }

  @Override
  public Vec advanceTo(Vec to) {
    return features.advanceTo(to);
  }

  @Override
  public int dim() {
    return features.dim();
  }

  @Override
  public FeatureMeta meta(int ind) {
    return features.meta(ind);
  }

  @Override
  public int index(FeatureMeta meta) {
    return features.index(meta);
  }

}
