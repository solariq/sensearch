package com.expleague.sensearch.index.embedding.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Embedding;

import java.util.*;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class EmbeddingImpl implements Embedding {

  private static final int VEC_SIZE = 50;

  private final Storage storage;

  public EmbeddingImpl(Config config) {
    //upload database
    storage = new Storage();
  }

  @Override
  public Vec getVec(long id) {
    return storage.get(id);
  }

  @Override
  public Vec getVec(LongStream ids) {
    return getArithmeticMean(ids.mapToObj(this::getVec));
  }

  private Vec getArithmeticMean(Stream<Vec> vecs) {
    ArrayVec mean = new ArrayVec(new double[VEC_SIZE]);
    long number = vecs.filter(Objects::nonNull).peek(vec -> mean.add((ArrayVec) vec)).count();
    if (number == 0) {
      return null;
    }
    mean.scale(1.0 / ((double) number));
    return mean;
  }

  @Override
  public LongStream getNearest(Vec mainVec, int numberOfNeighbors) {
    return storage.getNearest(mainVec.toArray(), numberOfNeighbors);
  }
}