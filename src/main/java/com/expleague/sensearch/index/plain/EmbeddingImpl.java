package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;

import com.expleague.sensearch.donkey.plain.ByteTools;
import com.expleague.sensearch.index.Embedding;
import com.google.common.primitives.Longs;
import gnu.trove.list.TLongList;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import static com.expleague.sensearch.donkey.plain.EmbeddingBuilder.hashFuncs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.LongStream;

public class EmbeddingImpl implements Embedding {

  private static final int VEC_SIZE = 50;
  private static final long CACHE_SIZE = 16 * (1 << 20);
  private static final double EPSILON = 10e-9;
  private static final Options DB_OPTIONS = new Options().cacheSize(CACHE_SIZE);

  private List<Long>[][] tables;
  private BiFunction<Vec, Vec, Double> nearestMeasure = VecTools::distanceAV;

  private DB vecDb;

  public EmbeddingImpl(Path embeddingPath) throws IOException {
    vecDb = Iq80DBFactory.factory.open(embeddingPath.toFile(), DB_OPTIONS);
    tables = null;
  }

  private Vec getVec(long id) {
    try {
      return ByteTools.toVec(vecDb.get(Longs.toByteArray(id)));
    } catch (DBException e) {
      return null;
    }
  }

  @Override
  public Vec getVec(long ... ids) {
    if (ids.length == 0) {
      return null;
    }

    if (ids.length == 1) {
      return getVec(ids[0]);
    }

    ArrayVec mean = new ArrayVec(new double[VEC_SIZE]);
    int number = 0;
    for (long id : ids) {
      Vec vec = getVec(id);
      if (vec != null) {
        mean.add((ArrayVec) vec);
        number++;
      }
    }

    mean.scale(1. / number);
    return mean;
  }

  @Override
  public Vec getVec(TLongList ids) {
    return getVec(ids.toArray());
  }

  private Comparator<Long> getComparator(Vec mainVec) {
    return (id1, id2) -> {
      double val1 = nearestMeasure.apply(mainVec, getVec(id1));
      double val2 = nearestMeasure.apply(mainVec, getVec(id2));
      if (Math.abs(val1 - val2) < EPSILON) {
        return 0;
      }
      return val1 < val2 ? -1 : 1;
    };
  }

  @Override
  public LongStream getNearest(Vec mainVec, int numberOfNeighbors) {
    List<Long> lshNeighbors = new ArrayList<>();
    for (int i = 0; i < tables.length; i++) {
      int bucketIndex = hashFuncs[i].applyAsInt(mainVec);
      lshNeighbors.addAll(tables[i][bucketIndex]);
    }

    Comparator<Long> comparator = getComparator(mainVec);

    if (lshNeighbors.size() <= numberOfNeighbors) {
      lshNeighbors.sort(comparator);
      return lshNeighbors.stream().mapToLong(Long::longValue);
    }

    TreeSet<Long> nearestNeighbors = new TreeSet<>(comparator);
    for (long id : lshNeighbors) {
      if (nearestNeighbors.size() < numberOfNeighbors) {
        nearestNeighbors.add(id);
      } else if (comparator.compare(nearestNeighbors.last(), id) > 0) {
        nearestNeighbors.pollLast();
        nearestNeighbors.add(id);
      }
    }

    return nearestNeighbors.stream().mapToLong(Long::longValue);
  }
}