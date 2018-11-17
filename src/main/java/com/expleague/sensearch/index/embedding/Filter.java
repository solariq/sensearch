package com.expleague.sensearch.index.embedding;

import com.expleague.commons.math.vectors.Vec;

import java.util.function.LongPredicate;
import java.util.stream.LongStream;

public interface Filter {

  LongStream filtrate(Vec mainVec, int number, LongPredicate predicate);
}