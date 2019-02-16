package com.expleague.sensearch.index;

import com.expleague.commons.math.vectors.Vec;
import java.util.List;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;
import org.jetbrains.annotations.Nullable;

public interface Embedding extends AutoCloseable {

  /**
   * Returns vector for an indexed item with given {@param id}. If this item does not have any
   * vector, returns null If this item has multiple vectors, returns first of them
   *
   * @param id indexed item id
   * @return first vector for this item or null if no vector exists
   */
  @Nullable
  Vec vec(long id);

  LongStream nearest(Vec mainVec, LongPredicate predicate);
  LongStream nearest(Vec mainVec, int numberOfNeighbors, LongPredicate predicate);
  LongStream nearest(Vec mainVec, double maxDistance, LongPredicate predicate);

  void setLSHFlag(boolean value);

  int dim();
}
