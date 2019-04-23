package com.expleague.sensearch.features.sets.ranker;

import com.expleague.sensearch.core.Term;

public interface WeightedTextFeatureSet extends TextFeatureSet {


  void withTermAndWeight(Term t, int offset, double weight);

  interface WeightedTextFeatureAccumulator extends TextFeatureAccumulator {

    void accept(Term term, double weight);

    double value(TermType type);
  }
}
