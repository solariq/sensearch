package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.nn.lsh.BaseQuantLSHCosIndex;
import com.expleague.commons.math.vectors.impl.nn.lsh.CosDistanceHashFunction;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.random.FastRandom;
import com.expleague.sensearch.donkey.plain.ByteTools;
import com.google.common.primitives.Longs;
import gnu.trove.list.array.TLongArrayList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.iq80.leveldb.DB;
import org.jetbrains.annotations.Nullable;

public class QuantLSHCosIndexDB extends BaseQuantLSHCosIndex implements AutoCloseable {
    private static final long SPECIAL_ID_1 = Long.MAX_VALUE - 3;
    private static final long SPECIAL_ID_2 = Long.MAX_VALUE - 2;
    private static final long SPECIAL_ID_3 = Long.MAX_VALUE - 1;
    private static final long SPECIAL_ID_4 = Long.MAX_VALUE;
    private final DB vecDB;

    public QuantLSHCosIndexDB(FastRandom rng, int quantDim, int dim, int minDist, DB vecDB) {
        super(rng, quantDim, dim, minDist);
        this.vecDB = vecDB;
    }

    public QuantLSHCosIndexDB(int dim, int minDist, TLongArrayList ids, List<TLongArrayList> sketches, CosDistanceHashFunction[] hashes, DB vecDB) {
        super(dim, minDist, ids, sketches, hashes);
        this.vecDB = vecDB;
    }

    @Override
    public Stream<Entry> nearest(Vec query) {
        return baseNearest(query, idx -> {
            long id = ids.getQuick(idx);
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
                Longs.toByteArray(SPECIAL_ID_1),
                ByteTools.toBytes(ids.toArray())
        );
        vecDB.put(
                Longs.toByteArray(SPECIAL_ID_2),
                ByteTools.toBytes(
                        IntStream.of(quantDim, dim, batchSize, sketches.size()).toArray()
                )
        );
        vecDB.put(
                Longs.toByteArray(SPECIAL_ID_3),
                ByteTools.toBytes(
                        sketches.stream().flatMapToLong(l -> LongStream.of(l.toArray())).toArray()
                )
        );
        vecDB.put(
                Longs.toByteArray(SPECIAL_ID_4),
                ByteTools.toBytes(
                        Arrays.stream(hashes).flatMapToDouble(h -> DoubleStream.of(h.randVec().toArray())).toArray()
                )

        );
        vecDB.close();
    }


    public static QuantLSHCosIndexDB load(DB vecDB) {
        TLongArrayList ids = new TLongArrayList(
                ByteTools.toLongArray(
                        vecDB.get(Longs.toByteArray(SPECIAL_ID_1))
                )
        );

        int[] fields = ByteTools.toIntArray(
                vecDB.get(Longs.toByteArray(SPECIAL_ID_2))
        );
        int quantDim = fields[0];
        int dim = fields[1];
        int minDist = fields[2];
        int sketchesSize = fields[3];

        long[] allSketches = ByteTools.toLongArray(vecDB.get(Longs.toByteArray(SPECIAL_ID_3)));
        List<TLongArrayList> sketches = new ArrayList<>();
        int chunkSize = allSketches.length / sketchesSize;
        for (int i = 0; i < sketchesSize; i++) {
            sketches.add(new TLongArrayList(Arrays.copyOfRange(allSketches, i * chunkSize, (i + 1) *chunkSize)));
        }

        double[] allCoords = ByteTools.toDoubleArray(vecDB.get(Longs.toByteArray(SPECIAL_ID_4)));
        CosDistanceHashFunction[] hashes = new CosDistanceHashFunction[SKETCH_BITS_PER_QUANT * (int)Math.ceil(dim / (double)quantDim)];
        for (int i = 0; i < dim; i += quantDim) {
            int finalI = i;
            final int currentDim = Math.min(quantDim, dim - i);
            for (int b = 0; b < SKETCH_BITS_PER_QUANT; b++) {
                int index = i / quantDim * SKETCH_BITS_PER_QUANT + b;
                final Vec w = new ArrayVec(Arrays.copyOfRange(allCoords, i * SKETCH_BITS_PER_QUANT + b * currentDim, i * SKETCH_BITS_PER_QUANT + (b + 1) * currentDim));
                hashes[index] = new CosDistanceHashFunction(w) { // quant sketch
                    @Override
                    public int hash(Vec v) {
                        return super.hash(v.sub(finalI, currentDim));
                    }
                };
            }
        }
        return new QuantLSHCosIndexDB(dim, 2000, ids, sketches, hashes, vecDB);
    }

    @Override
    public void close() throws Exception {
        vecDB.close();
    }
}