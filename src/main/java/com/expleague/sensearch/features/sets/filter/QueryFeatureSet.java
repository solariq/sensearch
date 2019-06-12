package com.expleague.sensearch.features.sets.filter;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.core.Term.TermAndDistance;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.query.Query;
import java.util.Collection;

public class QueryFeatureSet extends FeatureSet.Stub<QURLItem> {

  private static final FeatureMeta QUERY_LEN = FeatureMeta.create("query-len", "length of query", ValueType.VEC);
  private static final FeatureMeta MIN_SYN_DIST = FeatureMeta
      .create("min-syn-dist", "minimal synonim dist", ValueType.VEC);
  private static final FeatureMeta MAX_SYN_DIST = FeatureMeta
      .create("max-syn-dist", "maximal synonim dist", ValueType.VEC);

  private Query query;

  @Override
  public void accept(QURLItem item) {
    super.accept(item);

    this.query = item.queryCache();
  }

  @Override
  public Vec advance() {
    set(QUERY_LEN, query.terms().size());
    set(MIN_SYN_DIST, query.synonymsWithDistance().values().stream()
        .flatMap(Collection::stream)
        .mapToDouble(TermAndDistance::distance)
        .filter(x -> x > 1e-9)
        .min().orElse(1));
    set(MAX_SYN_DIST, query.synonymsWithDistance().values().stream()
        .flatMap(Collection::stream)
        .mapToDouble(TermAndDistance::distance)
        .max().orElse(0));
    return super.advance();
  }
}
