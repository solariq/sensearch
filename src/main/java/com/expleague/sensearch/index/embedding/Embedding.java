package com.expleague.sensearch.index.embedding;

import com.expleague.commons.math.vectors.Vec;

import java.util.stream.LongStream;

public interface Embedding {

  Vec getVec(long id);

  Vec getVec(LongStream ids);

  LongStream getNearest(Vec mainVec, int numberOfNeighbors);
}