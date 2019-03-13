package com.expleague.sensearch.snippet.features;

import com.expleague.commons.func.Functions;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.docbased_snippet.KeyWord;
import com.expleague.sensearch.snippet.passage.Passage;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AccumulatorFeatureSet extends FeatureSet.Stub<QPASItem> {

  private final Index index;
  private final FeatureSet<QPASItem> features = FeatureSet.join(
      new PassageBasedFeatureSet(),
      new SRWFeatureSet(),
      new CosDistanceFeatureSet(),
      new QuotationFeatureSet()
  );

  public AccumulatorFeatureSet(Index index) {
    this.index = index;
  }

  @Override
  public void accept(QPASItem item) {
    features.components()
        .map(Functions.cast(SRWFeatureSet.class))
        .filter(Objects::nonNull)
        .forEach(fs -> fs.withIndex(index));

    features.accept(item);

    { //COS DISTANCE
      Vec queryVec = index.vecByTerms(item.queryCache().terms());
      Vec passageVec = index.vecByTerms(item.passageCache().words().collect(Collectors.toList()));
      features.components()
          .map(Functions.cast(CosDistanceFeatureSet.class))
          .filter(Objects::nonNull)
          .forEach(fs -> fs.withVecs(queryVec, passageVec));
    }
    super.accept(item);
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
