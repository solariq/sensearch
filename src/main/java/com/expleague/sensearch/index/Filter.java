package com.expleague.sensearch.index;

import com.expleague.commons.math.vectors.Vec;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;
import org.jetbrains.annotations.NotNull;

public interface Filter {

  LongStream filtrate(@NotNull Vec mainVec, int number, LongPredicate predicate);
  LongStream filtrate(@NotNull Vec mainVec, double maxDistance, LongPredicate predicate);
}
