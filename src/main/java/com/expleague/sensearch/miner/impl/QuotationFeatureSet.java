package com.expleague.sensearch.miner.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.query.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class QuotationFeatureSet extends FeatureSet.Stub<QURLItem> {

  private static final FeatureMeta QUOTATION =
      FeatureMeta.create("quotations", "number of full quotation", ValueType.VEC);
  private static final FeatureMeta QUOTATIONL =
      FeatureMeta.create(
          "quotations-lemmas", "number of full quotation with lemmas", ValueType.VEC);

  private final List<List<Term>> passageTerms = new ArrayList<>();
  private Query query;

  @Override
  public void accept(QURLItem item) {
    passageTerms.clear();
    this.query = item.queryCache();
  }

  public void withPassage(List<Term> passage) {
    passageTerms.add(passage);
  }

  @Override
  public Vec advance() {
    List<Term> queryTerms = query.terms();
    List<Term> queryTermsWithLemmas =
        query.terms().stream().map(Term::lemma).collect(Collectors.toList());

    int[] result = new int[]{0, 0};
    passageTerms.forEach(
        passage -> {
          if (Collections.indexOfSubList(passage, queryTerms) != -1) {
            result[0]++;
          }
          List<Term> passageWithLemmas =
              passage.stream().map(Term::lemma).collect(Collectors.toList());
          if (Collections.indexOfSubList(passageWithLemmas, queryTermsWithLemmas) != -1) {
            result[1]++;
          }
        });
    set(QUOTATION, (double) result[0]);
    set(QUOTATIONL, (double) result[1]);
    return super.advance();
  }
}
