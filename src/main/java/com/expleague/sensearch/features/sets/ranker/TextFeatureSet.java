package com.expleague.sensearch.features.sets.ranker;

import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.features.QURLItem;

public interface TextFeatureSet extends FeatureSet<QURLItem> {

  void withStats(int totalLength, double averageTotalLength, int titleLength,
      double averageTitleLength, int contentLength,
      double averageContentLength,
      int indexLength);

  void withSegment(Segment type, Term t);

  void withTerm(Term t, int offset);

  enum Segment {
    TITLE,
    LINK,
    BODY
  }

  enum TermType {
    LEMMA,
    TERM
  }

  interface TextFeatureAccumulator {

    void accept(Term term);

    double value(TermType type);
  }
}
