package com.expleague.sensearch.index;

import com.expleague.commons.math.vectors.Vec;

import java.util.function.LongPredicate;
import java.util.stream.LongStream;

public interface Embedding {
  Vec vec(long id);
  LongStream nearest(Vec mainVec, int numberOfNeighbors, LongPredicate predicate);

  int dim();
}
