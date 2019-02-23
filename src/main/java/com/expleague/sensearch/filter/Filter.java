package com.expleague.sensearch.filter;

import com.expleague.commons.math.vectors.Vec;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.expleague.sensearch.index.plain.Candidate;
import org.jetbrains.annotations.NotNull;

public interface Filter {

  Stream<Candidate> filtrate(@NotNull Vec qVec, LongPredicate predicate);
  Stream<Candidate> filtrate(@NotNull Vec qVec, LongPredicate predicate, int number);
  Stream<Candidate> filtrate(@NotNull Vec qVec, LongPredicate predicate, double maxDistance);
}
