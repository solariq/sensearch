package com.expleague.sensearch.index;

import com.expleague.commons.math.vectors.Vec;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;
import org.jetbrains.annotations.Nullable;

public interface Embedding {

  @Nullable
  Vec vec(long id);

  LongStream nearest(Vec mainVec, int numberOfNeighbors, LongPredicate predicate);
  LongStream nearest(Vec mainVec, double maxDistance, LongPredicate predicate);

  int dim();
}
