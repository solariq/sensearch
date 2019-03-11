package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.nn.NearestNeighbourIndex;
import com.expleague.commons.util.ArrayTools;
import com.expleague.commons.util.cache.CacheStrategy.Type;
import com.expleague.commons.util.cache.impl.FixedSizeCache;
import com.expleague.sensearch.core.Annotations.EmbeddingVecsDb;
import com.expleague.sensearch.core.Annotations.UseLshFlag;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.index.Embedding;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.log4j.Logger;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

public class EmbeddingImpl implements Embedding {

  private static final Logger LOG = Logger.getLogger(EmbeddingImpl.class);

  private static final int CACHE_SIZE = 64 * (1 << 10); // 64K vecs, 64 * 128 * 8 = 64Mb

  private final FixedSizeCache<Long, Vec> vecCache = new FixedSizeCache<>(CACHE_SIZE, Type.LRU);

  private long[] allIds;
  private final boolean defaultLshFlag;
  private final QuantLSHCosIndexDB nnIdx;

  @Inject
  public EmbeddingImpl(@UseLshFlag boolean useLshFlag, @EmbeddingVecsDb DB vecDb) {
    defaultLshFlag = useLshFlag;
    nnIdx = QuantLSHCosIndexDB.load(vecDb);

    if (!useLshFlag) {
      TLongList idList = new TLongArrayList();
      try (DBIterator iterator = vecDb.iterator();) {
        for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
          byte[] key = iterator.peekNext().getKey();
          long id = Longs.fromByteArray(key);
          if (id < Long.MAX_VALUE - 3) {
            idList.add(id);
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      allIds = idList.toArray();
    }
  }

  @Override
  public Vec vec(long id) {
    return vecCache.get(id, nnIdx::get);
  }

  @Override
  public Stream<Candidate> nearest(Vec qVec, LongPredicate predicate) {
    return nearest(qVec, predicate, defaultLshFlag);
  }

  @Override
  public Stream<Candidate> nearest(Vec qVec, LongPredicate predicate, int numOfNeighbors) {
    return nearest(qVec, predicate, numOfNeighbors, defaultLshFlag);
  }

  @Override
  public Stream<Candidate> nearest(Vec qVec, LongPredicate predicate, double maxDist) {
    return nearest(qVec, predicate, maxDist, defaultLshFlag);
  }

  public Stream<Candidate> nearest(Vec qVec, LongPredicate predicate, boolean approximate) {
    return baseNearest(qVec, predicate, Integer.MAX_VALUE, Double.POSITIVE_INFINITY, approximate);
  }

  public Stream<Candidate> nearest(
      Vec qVec, LongPredicate predicate, int numOfNeighbors, boolean approximate) {
    return baseNearest(qVec, predicate, numOfNeighbors, Double.POSITIVE_INFINITY, approximate);
  }

  public Stream<Candidate> nearest(
      Vec qVec, LongPredicate predicate, double maxDist, boolean approximate) {
    return baseNearest(qVec, predicate, Integer.MAX_VALUE, maxDist, approximate);
  }

  private Stream<Candidate> baseNearest(
      Vec qVec, LongPredicate predicate, int numOfNeighbors, double maxDist, boolean approximate) {
    final double qNorm = VecTools.norm(qVec);
    if (qNorm == 0) {
      return Stream.empty();
    }

    if (!approximate) {
      final long[] ids = LongStream.of(allIds).filter(predicate).distinct().toArray();
      final double[] dists =
          LongStream.of(ids).mapToDouble(id -> distance(qVec, qNorm, vec(id))).toArray();
      ArrayTools.parallelSort(dists, ids);
      int count =
          Math.min(
              ids.length,
              numOfNeighbors < ids.length ? numOfNeighbors : Arrays.binarySearch(dists, maxDist));

      List<Candidate> candidates = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        candidates.add(new Candidate(ids[i], dists[i]));
      }
      return candidates.stream();
    }

    final Spliterator<NearestNeighbourIndex.Entry> spliterator =
        nnIdx.nearest(qVec, predicate).spliterator();
    return StreamSupport.stream(
        new Spliterators.AbstractSpliterator<Candidate>(
            numOfNeighbors, Spliterator.IMMUTABLE | Spliterator.NONNULL) {
          boolean eos = false;
          int count = 0;

          @Override
          public boolean tryAdvance(Consumer<? super Candidate> action) {
            if (eos) {
              return false;
            }
            long startTime = System.nanoTime();
            spliterator.tryAdvance(
                entry -> {
                  if (++count > numOfNeighbors) {
                    eos = true;
                  }
                  if (entry.distance() > maxDist) {
                    eos = true;
                  }
                  if (!eos) {
                    action.accept(new Candidate(entry.id(), entry.distance()));
                  }
                });
            return !eos;
          }
        },
        false);
  }

  private double distance(Vec qVec, double qNorm, Vec v) {
    double norm = VecTools.norm(v);
    return norm == 0
        ? Double.POSITIVE_INFINITY
        : (1 - VecTools.multiply(qVec, v) / norm / qNorm) / 2;
  }

  @Override
  public int dim() {
    return PlainIndexBuilder.DEFAULT_VEC_SIZE;
  }

  @Override
  public void close() throws Exception {
    nnIdx.close();
  }
}
