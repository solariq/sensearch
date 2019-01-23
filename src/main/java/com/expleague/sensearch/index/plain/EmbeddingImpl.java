package com.expleague.sensearch.index.plain;

import static com.expleague.sensearch.donkey.plain.EmbeddingBuilder.TUPLE_SIZE;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.commons.util.ArrayTools;
import com.expleague.sensearch.donkey.plain.ByteTools;
import com.expleague.sensearch.donkey.plain.EmbeddingBuilder;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.index.Embedding;
import com.google.common.primitives.Longs;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.LongPredicate;
import java.util.function.ToLongFunction;
import java.util.stream.LongStream;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;

public class EmbeddingImpl implements Embedding {
  private static final long CACHE_SIZE = 16 * (1 << 20);
  private static final Options DB_OPTIONS = new Options().cacheSize(CACHE_SIZE);
  private static final int MAX_DIFFERENT_BITS = 5;
  private final long[] allIds;

  private BiFunction<Vec, Vec, Double> nearestMeasure = VecTools::distanceAV;

  private DB vecDB, tablesDB;
  private ToLongFunction<Vec>[] hashFuncs;

  public EmbeddingImpl(Path embeddingPath) throws IOException {
    vecDB =
            JniDBFactory.factory.open(
                embeddingPath.resolve(PlainIndexBuilder.VECS_ROOT).toFile(), DB_OPTIONS);

    tablesDB =
            JniDBFactory.factory.open(
                embeddingPath.resolve(PlainIndexBuilder.LSH_ROOT).toFile(), DB_OPTIONS);

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
    {
      TLongArrayList allIds = new TLongArrayList();
      DBIterator iterator = vecDB.iterator();
      iterator.seekToFirst();
      while (iterator.hasNext()) {
        Map.Entry<byte[], byte[]> next = iterator.next();
        long id = Longs.fromByteArray(next.getKey());
        allIds.add(id);
      }
      this.allIds = allIds.toArray();
    }


    hashFuncs = new ToLongFunction[EmbeddingBuilder.TABLES_NUMBER];
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

                long hash = ((long) hashNum) << ((long) TUPLE_SIZE);
                for (int j = 0; j < mask.length; j++) {
                  if (mask[j]) {
                    hash += 1L << ((long) j);
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
      List<Vec> vecs = ByteTools.toVecs(bytes);
      return vecs.size() == 0 ? null : vecs.get(0);
    }
    return null;
  }

  @Override
  public List<Vec> allVecs(long id) {
    byte[] bytes = vecDB.get(Longs.toByteArray(id));
    if (bytes != null) {
      return ByteTools.toVecs(bytes);
    }
    return Collections.emptyList();
  }

  private static void nearestIndexes(TLongList nearestIndexes, long index, int pos, int remaining) {
    if (remaining == 0) {
      nearestIndexes.add(index);
      return;
    }
    if (remaining > TUPLE_SIZE - pos) {
      return;
    }
    nearestIndexes(nearestIndexes, index, pos + 1, remaining);
    index = index ^ (1L << ((long) pos));
    nearestIndexes(nearestIndexes, index, pos + 1, remaining - 1);
  }

  @Override
  public LongStream nearest(Vec mainVec, int numberOfNeighbors, LongPredicate predicate) {
    final long[] order = LongStream.of(allIds).filter(predicate).toArray();
    if (order.length <= numberOfNeighbors) {
      return LongStream.of(order);
    }
    final double[] dist = LongStream.of(order)
        .mapToObj(this::allVecs)
        .flatMap(List::stream)
        .mapToDouble(v -> -VecTools.multiply(mainVec, v))
        .toArray();
    ArrayTools.parallelSort(dist, order);
    return Arrays.stream(order).distinct().limit(numberOfNeighbors);
  }

  @Override
  public LongStream nearest(Vec mainVec, double maxDistance, LongPredicate predicate) {
    final double queryNorm = VecTools.norm(mainVec);
    if (queryNorm == 0)
      return LongStream.empty();
    TLongList orderList = new TLongArrayList();
    final double[] dist = LongStream.of(allIds).filter(predicate)
        .mapToObj(id -> {
          List<Vec> vecs = allVecs(id);
          for (int i = 0; i < vecs.size(); i++) {
            orderList.add(id);
          }

          return vecs;
        })
        .flatMap(List::stream)
        .mapToDouble(v -> distance(mainVec, queryNorm, v))
        .toArray();
    final long[] order = orderList.toArray();
    ArrayTools.parallelSort(dist, order);
    int end = Arrays.binarySearch(dist, maxDistance);
    if (end < 0)
      end = -end - 1;
    return Arrays.stream(order).distinct().limit(end);
  }

  private double distance(Vec mainVec, double queryNorm, Vec v) {
    double norm = VecTools.norm(v);
    return norm == 0 ? Double.POSITIVE_INFINITY : (1 - VecTools.multiply(mainVec, v)/ norm / queryNorm) / 2;
  }

  @Override
  public int dim() {
    return PlainIndexBuilder.DEFAULT_VEC_SIZE;
  }
}
