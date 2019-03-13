package com.expleague.sensearch.snippet.features;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.passage.Passage;
import com.expleague.sensearch.snippet.passage.Passages;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class QuotationFeatureSet extends FeatureSet.Stub<QPASItem> {

  private static final FeatureMeta QUERY_WORDS = FeatureMeta
      .create("query words", "Words from query in passage", ValueType.VEC);
  private static final FeatureMeta QUOTATION = FeatureMeta
      .create("quotation", "Quotation of query in passage", ValueType.VEC);

  private Query query;
  private Passage passage;

  @Override
  public void accept(QPASItem item) {
    this.query = item.queryCache();
    this.passage = item.passageCache();
    super.accept(item);
  }

  @Override
  public Vec advance() {
    long count = query.terms()
        .stream()
        .filter(queryTerm -> Passages.containsWithLemma(passage, queryTerm))
        .count();
    set(QUERY_WORDS, (double) count);

    List<Term> queryTermsWithLemmas = query.terms()
        .stream()
        .map(Term::lemma)
        .collect(Collectors.toList());

    List<Term> passageTermsWithLemmas = passage.words()
        .map(Term::lemma)
        .collect(Collectors.toList());

    set(QUOTATION,
        Collections.indexOfSubList(passageTermsWithLemmas, queryTermsWithLemmas) != -1 ? 1. : 0.);
    return super.advance();
  }
}