package com.expleague.sensearch.miner.impl;

import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.sensearch.core.Term;

public interface TextFeatureSet extends FeatureSet<QURLItem> {
  void withStats(int pageLen, double avgLen, int indexLen);
  void withSegment(Segment type, int length);
  void withTerm(Term t, int offset);

  enum Segment {
    TITLE,
    LINK,
    BODY
  }

  interface TextFeatureAccumulator {
    void accept(Term term);
    double value();
  }
}
