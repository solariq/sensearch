package com.expleague.sensearch.index.embedding.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.index.embedding.Embedding;

import gnu.trove.list.TLongList;
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
  public Vec getVec(long ... ids) {
    if (ids.length == 0) {
      // TODO: determine behaviour
    }

    if (ids.length == 1) {
      return storage.get(ids[0]);
    }

    ArrayVec vectors = new ArrayVec(new double[VEC_SIZE]);
    int vectorsFound = 0;
    for (int i = 0; i < ids.length; ++i) {
      Vec vec = storage.get(ids[i]);
      if (vec != null) {
        vectors.add((ArrayVec) vec);
        ++vectorsFound;
      }
    }

    vectors.scale(1. / vectorsFound);

    return vectors;

  }

  @Override
  public Vec getVec(TLongList ids) {
    return getVec(ids.toArray());
  }

  @Override
  public LongStream getNearest(Vec mainVec, int numberOfNeighbors) {
    return storage.getNearest(mainVec.toArray(), numberOfNeighbors);
  }
}