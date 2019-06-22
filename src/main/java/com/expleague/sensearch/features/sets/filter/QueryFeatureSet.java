package com.expleague.sensearch.features.sets.filter;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.core.Term.TermAndDistance;
import com.expleague.sensearch.experiments.Main;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.query.Query;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public class QueryFeatureSet extends FeatureSet.Stub<QURLItem> {

  private static final FeatureMeta QUERY_LEN = FeatureMeta.create("query-len", "length of query", ValueType.VEC);
  private static final FeatureMeta MIN_SYN_DIST = FeatureMeta
      .create("min-syn-dist", "minimal synonim dist", ValueType.VEC);
  private static final FeatureMeta MAX_SYN_DIST = FeatureMeta
      .create("max-syn-dist", "maximal synonim dist", ValueType.VEC);
  private static final FeatureMeta SQC = FeatureMeta
          .create("sqc", "similarity of query and collection", ValueType.VEC);
  private static final FeatureMeta NSQC = FeatureMeta
          .create("nsqc", "similarity of query and collection, divided by the query length", ValueType.VEC);
  private static final FeatureMeta MAX_SQC = FeatureMeta
          .create("max-sqc", "similarity of query and collection by best term", ValueType.VEC);

  private Query query;
  private Index index;

  @Override
  public void accept(QURLItem item) {
    super.accept(item);
    this.query = item.queryCache();
  }

  public void withIndex(Index index) {
    this.index = index;
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

    ToDoubleFunction<Term> f = t -> (1 + Math.log(t.freq())) * (t.documentFreq() == 0 ? 1.0 : Math.log(1 + index.size() / t.documentFreq()));
    double sqc = query.terms().stream().mapToDouble(f).sum();
    OptionalDouble maxSQC = query.terms().stream().mapToDouble(f).max();
    set(SQC, sqc);
    set(NSQC, sqc / query.terms().stream().filter(t -> t.freq() > 0).count());
    set(MAX_SQC, maxSQC.isPresent() ? maxSQC.getAsDouble() : 0.0);
    return super.advance();
  }

}
