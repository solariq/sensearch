package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.nn.lsh.BaseQuantLSHCosIndex;
import com.expleague.commons.math.vectors.impl.nn.lsh.CosDistanceHashFunction;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.random.FastRandom;
import com.expleague.sensearch.core.ByteTools;
import com.google.common.primitives.Longs;
import gnu.trove.list.array.TLongArrayList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.apache.log4j.Logger;
import org.iq80.leveldb.DB;
import org.jetbrains.annotations.Nullable;

public class QuantLSHCosIndexDB extends BaseQuantLSHCosIndex implements AutoCloseable {

  private static final Logger LOG = Logger.getLogger(QuantLSHCosIndexDB.class);

  private static final long FIELDS_ID = Long.MAX_VALUE;
  private static final long IDS_ID = Long.MAX_VALUE - 1;
  private static final long HASHES_ID = Long.MAX_VALUE - 2;
  private static final long SKETCHES_ID = Long.MAX_VALUE - 3;
  private final DB vecDB;

  public QuantLSHCosIndexDB(FastRandom rng, int dim, int quantDim, int sketchBitsPerQuant, int batchSize, DB vecDB) {
    super(rng, dim, quantDim, sketchBitsPerQuant, batchSize);
    this.vecDB = vecDB;
  }

  public QuantLSHCosIndexDB(int dim, int batchSize, TLongArrayList ids, List<TLongArrayList> sketches, CosDistanceHashFunction[] hashes, DB vecDB) {
    super(dim, batchSize, ids, sketches, hashes);
    this.vecDB = vecDB;
  }

  @Override
  public Stream<Entry> nearest(Vec query) {
    return nearest(query, value -> true);
  }

  public Stream<Entry> nearest(Vec query, LongPredicate idCondition) {
    return baseNearest(query, idx -> {
      long id = ids.getQuick(idx);
      if (!idCondition.test(id))
        return null;
      return VecTools.normalizeL2(ByteTools.toVec(vecDB.get(Longs.toByteArray(id))));
    });
  }

  @Override
  public synchronized void append(long id, Vec vec) {
    baseAppend(id, vec);
    vecDB.put(Longs.toByteArray(id), ByteTools.toBytes(vec));
  }

  @Override
  public synchronized void remove(long id) {
    baseRemove(id);
    vecDB.delete(Longs.toByteArray(id));
  }

  @Nullable
  public Vec get(long id) {
    byte[] bytes = vecDB.get(Longs.toByteArray(id));
    if (bytes != null) {
      return ByteTools.toVec(bytes);
    }
    return null;
  }

  public void save() throws IOException {
    vecDB.put(
        Longs.toByteArray(FIELDS_ID),
        ByteTools.toBytes(
            IntStream.of(dim, quantDim, sketchBitsPerQuant, batchSize).toArray()
        )
    );
    vecDB.put(
        Longs.toByteArray(IDS_ID),
        ByteTools.toBytes(ids.toArray())
    );
    vecDB.put(
        Longs.toByteArray(HASHES_ID),
        ByteTools.toBytes(
            Arrays.stream(hashes).flatMapToDouble(h -> DoubleStream.of(h.randVec().toArray())).toArray()
        )

    );
    vecDB.put(
        Longs.toByteArray(SKETCHES_ID),
        ByteTools.toBytes(
            sketches.stream().flatMapToLong(l -> LongStream.of(l.toArray())).toArray()
        )
    );
    vecDB.close();
  }


  public static QuantLSHCosIndexDB load(DB vecDB) {
    LOG.info("Loading LSH...");

    int[] fields = ByteTools.toIntArray(
        vecDB.get(Longs.toByteArray(FIELDS_ID))
    );
    int dim = fields[0];
    int quantDim = fields[1];
    int sketchBitsPerQuant = fields[2];
    int batchSize = fields[3];

    TLongArrayList ids = new TLongArrayList(
        ByteTools.toLongArray(
            vecDB.get(Longs.toByteArray(IDS_ID))
        )
    );

    double[] allCoords = ByteTools.toDoubleArray(vecDB.get(Longs.toByteArray(HASHES_ID)));
    CosDistanceHashFunction[] hashes = new CosDistanceHashFunction[sketchBitsPerQuant * (int) Math.ceil(dim / (double)quantDim)];
    for (int i = 0; i < dim; i += quantDim) {
      int finalI = i;
      final int currentDim = Math.min(quantDim, dim - i);
      for (int b = 0; b < sketchBitsPerQuant; b++) {
        int index = i / quantDim * sketchBitsPerQuant + b;
        final Vec w = new ArrayVec(Arrays.copyOfRange(allCoords, i * sketchBitsPerQuant + b * currentDim, i * sketchBitsPerQuant + (b + 1) * currentDim));
        hashes[index] = new CosDistanceHashFunction(w) { // quant sketch
          @Override
          public int hash(Vec v) {
            return super.hash(v.sub(finalI, currentDim));
          }
        };
      }
    }

    long[] allSketches = ByteTools.toLongArray(vecDB.get(Longs.toByteArray(SKETCHES_ID)));
    List<TLongArrayList> sketches = new ArrayList<>();
    int sketchesSize = (int) Math.ceil(hashes.length / 64.);
    int chunkSize = allSketches.length / sketchesSize;
    for (int i = 0; i < sketchesSize; i++) {
      sketches.add(new TLongArrayList(Arrays.copyOfRange(allSketches, i * chunkSize, (i + 1) * chunkSize)));
    }

    return new QuantLSHCosIndexDB(dim, batchSize, ids, sketches, hashes, vecDB);
  }

  @Override
  public void close() throws IOException {
    vecDB.close();
  }
}