package com.expleague.sensearch.index;

import com.expleague.commons.math.vectors.Vec;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.expleague.sensearch.index.plain.Candidate;
import org.jetbrains.annotations.NotNull;

public interface Filter {

  Stream<Candidate> filtrate(@NotNull Vec qVec, LongPredicate predicate);
  Stream<Candidate> filtrate(@NotNull Vec qVec, int number, LongPredicate predicate);
  Stream<Candidate> filtrate(@NotNull Vec qVec, double maxDistance, LongPredicate predicate);
}
