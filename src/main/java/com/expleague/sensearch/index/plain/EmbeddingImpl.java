package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;

import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.donkey.plain.ByteTools;
import com.expleague.sensearch.donkey.plain.EmbeddingBuilder;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.index.Embedding;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import gnu.trove.list.TLongList;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;
import java.util.stream.LongStream;

public class EmbeddingImpl implements Embedding {

  private static final long CACHE_SIZE = 16 * (1 << 20);
  private static final double EPSILON = 1e-9;
  private static final Options DB_OPTIONS = new Options().cacheSize(CACHE_SIZE);

  private BiFunction<Vec, Vec, Double> nearestMeasure = VecTools::distanceAV;

  private DB vecDB, tablesDB;
  private ToIntFunction<Vec>[] hashFuncs;

  public EmbeddingImpl(Path embeddingPath) throws IOException {
    vecDB = JniDBFactory.factory.open(
            embeddingPath.resolve(EmbeddingBuilder.VECS_ROOT).toFile(),
            DB_OPTIONS
    );

    tablesDB = JniDBFactory.factory.open(
            embeddingPath.resolve(EmbeddingBuilder.LSH_ROOT).toFile(),
            DB_OPTIONS
    );

    List<Vec> randVecs = new ArrayList<>();
    try (Reader input =
        new InputStreamReader(
            new FileInputStream(
                embeddingPath.resolve(EmbeddingBuilder.RAND_VECS).toFile()
            )
        )
    ) {
      CharSeqTools.lines(input)
          .forEach(line -> {
              CharSequence[] parts = CharSeqTools.split(line, ' ');
                  randVecs.add(
                      new ArrayVec(
                          Arrays.stream(parts).mapToDouble(CharSeqTools::parseDouble).toArray()
                      )
                  );
                }
              );
    }

    hashFuncs = new ToIntFunction[EmbeddingBuilder.TABLES_NUMBER];
    for (int i = 0; i < hashFuncs.length; i++) {

      final int hashNum = i;
      hashFuncs[i] = (vec) -> {

        boolean[] mask = new boolean[EmbeddingBuilder.TUPLE_SIZE];
        for (int j = 0; j < mask.length; j++) {
          mask[j] = VecTools.multiply(vec, randVecs.get(EmbeddingBuilder.TUPLE_SIZE * hashNum + j)) >= 0;
        }

        int hash = (1 << EmbeddingBuilder.TUPLE_SIZE) * hashNum;
        for (int j = 0; j < mask.length; j++) {
          if (mask[j]) {
            hash += 1 << j;
          }
        }

        return hash;
      };
    }
  }

  private Vec getVec(long id) {
    byte[] bytes = vecDB.get(Longs.toByteArray(id));
    if (bytes != null) {
      return ByteTools.toVec(bytes);
    }
    return null;
  }

  @Override
  public Vec getVec(long ... ids) {
    if (ids.length == 0) {
      return null;
    }

    if (ids.length == 1) {
      return getVec(ids[0]);
    }

    ArrayVec mean = new ArrayVec(new double[PlainIndexBuilder.DEFAULT_VEC_SIZE]);
    int number = 0;
    for (long id : ids) {
      Vec vec = getVec(id);
      if (vec != null) {
        mean.add((ArrayVec) vec);
        number++;
      }
    }

    if (number == 0) {
      return null;
    }

    mean.scale(1. / (double) number);
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
    Comparator<Long> comparator = getComparator(mainVec);

    Set<Long> lshNeighbors = new TreeSet<>(comparator);
    for (ToIntFunction<Vec> hashFunc : hashFuncs) {
      int bucketIndex = hashFunc.applyAsInt(mainVec);
      long[] ids = ByteTools.toLongArray(tablesDB.get(Ints.toByteArray(bucketIndex)));
      for (long id : ids) {
        lshNeighbors.add(id);
      }
    }

    if (lshNeighbors.size() <= numberOfNeighbors) {
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