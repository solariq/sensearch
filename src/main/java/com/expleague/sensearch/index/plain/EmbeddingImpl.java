package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.commons.util.ArrayTools;
import com.expleague.sensearch.donkey.plain.ByteTools;
import com.expleague.sensearch.donkey.plain.EmbeddingBuilder;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.index.Embedding;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import static com.expleague.sensearch.donkey.plain.EmbeddingBuilder.TUPLE_SIZE;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.LongPredicate;
import java.util.function.ToIntFunction;
import java.util.stream.LongStream;

public class EmbeddingImpl implements Embedding {
  private static final long CACHE_SIZE = 16 * (1 << 20);
  private static final Options DB_OPTIONS = new Options().cacheSize(CACHE_SIZE);
  private static final int MAX_DIFFERENT_BITS = 5;

  private BiFunction<Vec, Vec, Double> nearestMeasure = VecTools::distanceAV;

  private DB vecDB, tablesDB;
  private ToIntFunction<Vec>[] hashFuncs;

  public EmbeddingImpl(Path embeddingPath) throws IOException {
    vecDB =
        JniDBFactory.factory.open(
            embeddingPath.resolve(EmbeddingBuilder.VECS_ROOT).toFile(), DB_OPTIONS);

    tablesDB =
        JniDBFactory.factory.open(
            embeddingPath.resolve(EmbeddingBuilder.LSH_ROOT).toFile(), DB_OPTIONS);

    List<Vec> randVecs = new ArrayList<>();
    try (Reader input =
        new InputStreamReader(
            new FileInputStream(embeddingPath.resolve(EmbeddingBuilder.RAND_VECS).toFile()))) {
      CharSeqTools.lines(input)
          .forEach(
              line -> {
                CharSequence[] parts = CharSeqTools.split(line, ' ');
                randVecs.add(
                    new ArrayVec(
                        Arrays.stream(parts).mapToDouble(CharSeqTools::parseDouble).toArray()));
              });
    }

    hashFuncs = new ToIntFunction[EmbeddingBuilder.TABLES_NUMBER];
    for (int i = 0; i < hashFuncs.length; i++) {

      final int hashNum = i;
      hashFuncs[i] =
          (vec) -> {
            boolean[] mask = new boolean[TUPLE_SIZE];
            for (int j = 0; j < mask.length; j++) {
              mask[j] =
                  VecTools.multiply(vec, randVecs.get(TUPLE_SIZE * hashNum + j))
                      >= 0;
            }

            int hash = (hashNum << TUPLE_SIZE);
            for (int j = 0; j < mask.length; j++) {
              if (mask[j]) {
                hash += 1 << j;
              }
            }

            return hash;
          };
    }
  }

  @Override
  public Vec vec(long id) {
    byte[] bytes = vecDB.get(Longs.toByteArray(id));
    if (bytes != null) {
      return ByteTools.toVec(bytes);
    }
    return null;
  }

  private static void nearestIndexes(TIntList nearestIndexes, int index, int pos, int remaining) {
    if (remaining == 0) {
      nearestIndexes.add(index);
      return;
    }
    if (remaining > TUPLE_SIZE - pos) {
      return;
    }
    nearestIndexes(nearestIndexes, index, pos + 1, remaining);
    index = (index ^ (1 << pos));
    nearestIndexes(nearestIndexes, index, pos + 1, remaining - 1);
  }

  @Override
  public LongStream nearest(Vec mainVec, int numberOfNeighbors, LongPredicate predicate) {
    int[] indexes = new int[hashFuncs.length];
    for (int i = 0; i < hashFuncs.length; i++) {
      indexes[i] = hashFuncs[i].applyAsInt(mainVec);
    }

    final TLongSet lshNeighbors = new TLongHashSet();
    TIntList nearestIndexes = new TIntArrayList();
    for (int diffBitsN = 0; diffBitsN <= MAX_DIFFERENT_BITS; diffBitsN++) {
      for (int index : indexes) {
        nearestIndexes.clear();
        nearestIndexes(nearestIndexes, index, 0, diffBitsN);
        for (int i = 0; i < nearestIndexes.size(); i++) {
          LongStream.of(
                  ByteTools.toLongArray(
                          tablesDB.get(
                                  Ints.toByteArray(
                                          nearestIndexes.get(i)
                                  )
                          )
                  )
          ).forEach(lshNeighbors::add);
        }
      }
      if (lshNeighbors.size() >= numberOfNeighbors) {
        break;
      }
    }

    final long[] order = LongStream.of(lshNeighbors.toArray()).filter(predicate).toArray();
    if (order.length <= numberOfNeighbors) {
      return LongStream.of(order);
    }

    final double[] dist = LongStream.of(order)
        .mapToObj(this::vec)
        .mapToDouble(v -> -VecTools.cosine(mainVec, v))
        .toArray();
    ArrayTools.parallelSort(dist, order);
    return Arrays.stream(order, 0, numberOfNeighbors);
  }

  @Override
  public int dim() {
    return PlainIndexBuilder.DEFAULT_VEC_SIZE;
  }
/*
  public static void main(String[] args) {
    *//*TIntList list = new TIntArrayList();
    nearestIndexes(list, 26, 0, 2);
    for (int i = 0; i < list.size(); i++) {
      System.out.println(list.get(i));
    }*//*
    int prev = -1;
    for (int i = 1; i <= 40; i++) {
      int cur = (10 << i);
      if (cur < prev) {
        System.out.println(i);
        System.out.println(cur);
        break;
      }
      System.out.println(cur);
      prev = cur;
    }
  }*/
}
