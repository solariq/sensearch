package com.expleague.sensearch.index;

import com.expleague.commons.math.vectors.Vec;
import java.util.List;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.expleague.sensearch.index.plain.Candidate;
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

  Stream<Candidate> nearest(Vec qVec, LongPredicate predicate);
  Stream<Candidate> nearest(Vec qVec, int numberOfNeighbors, LongPredicate predicate);
  Stream<Candidate> nearest(Vec qVec, double maxDistance, LongPredicate predicate);

  void setLSHFlag(boolean value);

  int dim();
}
