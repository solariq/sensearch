package com.expleague.sensearch.index.plain;

import static com.expleague.sensearch.donkey.plain.EmbeddingBuilder.TABLES_NUMBER;
import static com.expleague.sensearch.donkey.plain.EmbeddingBuilder.TUPLE_SIZE;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.commons.util.ArrayTools;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Annotations.EmbeddingLshTablesDb;
import com.expleague.sensearch.core.Annotations.EmbeddingPath;
import com.expleague.sensearch.core.Annotations.EmbeddingVecsDb;
import com.expleague.sensearch.donkey.plain.ByteTools;
import com.expleague.sensearch.donkey.plain.EmbeddingBuilder;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.index.Embedding;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
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
import java.util.function.LongPredicate;
import java.util.function.ToLongFunction;
import java.util.stream.LongStream;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

public class EmbeddingImpl implements Embedding {
    private static final int MAX_DIFFERENT_BITS = 4;
    private long[] allIds;

    private DB vecDB, tablesDB;
    private boolean lshFlag;
    private ToLongFunction<Vec>[] hashFuncs;

    @Inject
    public EmbeddingImpl(
            Config config,
            @EmbeddingVecsDb DB vecsDb,
            @EmbeddingLshTablesDb DB tablesDB,
            @EmbeddingPath Path embeddingPath)
            throws IOException {
        lshFlag = config.getLshNearestFlag();
        this.vecDB = vecsDb;
        this.tablesDB = tablesDB;

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

        hashFuncs = new ToLongFunction[EmbeddingBuilder.TABLES_NUMBER];
        for (int i = 0; i < hashFuncs.length; i++) {
            final int hashNum = i;
            hashFuncs[i] = (vec) -> {
                boolean[] mask = new boolean[TUPLE_SIZE];
                for (int j = 0; j < mask.length; j++) {
                    mask[j] = VecTools.multiply(vec, randVecs.get(TUPLE_SIZE * hashNum + j)) >= 0;
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

    private long[] lshNearest(Vec mainVec) {
        long[] indexes = new long[hashFuncs.length];
        for (int i = 0; i < hashFuncs.length; i++) {
            indexes[i] = hashFuncs[i].applyAsLong(mainVec);
        }

        final TLongSet lshNeighbors = new TLongHashSet();
        TLongList nearestIndexes = new TLongArrayList();
        for (int diffBitsN = 0; diffBitsN <= MAX_DIFFERENT_BITS; diffBitsN++) {
            for (long index : indexes) {
                nearestIndexes.clear();
                nearestIndexes(nearestIndexes, index, 0, diffBitsN);
                for (int i = 0; i < nearestIndexes.size(); i++) {
                    byte[] bytes = tablesDB.get(
                            Longs.toByteArray(
                                    nearestIndexes.get(i)
                            )
                    );
                    if (bytes != null) {
                        LongStream.of(
                                ByteTools.toLongArray(bytes)
                        ).forEach(lshNeighbors::add);
                    }
                }
            }
        }
        return lshNeighbors.toArray();
    }

    private double[] getDists(
            Vec mainVec, LongPredicate predicate, double queryNorm, TLongList orderList) {
        long[] ids = lshFlag ? lshNearest(mainVec) : allIds;
        return LongStream.of(ids)
                .filter(predicate)
                .mapToObj(
                        id -> {
                            List<Vec> vecs = allVecs(id);
                            for (int i = 0; i < vecs.size(); i++) {
                                orderList.add(id);
                            }

                            return vecs;
                        })
                .flatMap(List::stream)
                .mapToDouble(v -> distance(mainVec, queryNorm, v))
                .toArray();
    }

    @Override
    public void setLSHFlag(boolean value) {
        lshFlag = value;
    }

    @Override
    public LongStream nearest(Vec mainVec, LongPredicate predicate) {
        long[] ids = lshFlag ? lshNearest(mainVec) : allIds;
        return LongStream.of(ids).filter(predicate);
    }

    @Override
    public LongStream nearest(Vec mainVec, int numberOfNeighbors, LongPredicate predicate) {
        final double queryNorm = VecTools.norm(mainVec);
        if (queryNorm == 0) {
            return LongStream.empty();
        }

        TLongList orderList = new TLongArrayList();
        final double[] dist = getDists(mainVec, predicate, queryNorm, orderList);
        final long[] order = orderList.toArray();
        ArrayTools.parallelSort(dist, order);

        return Arrays.stream(order).distinct().limit(numberOfNeighbors);
    }

    @Override
    public LongStream nearest(Vec mainVec, double maxDistance, LongPredicate predicate) {
        final double queryNorm = VecTools.norm(mainVec);
        if (queryNorm == 0) {
            return LongStream.empty();
        }

        TLongList orderList = new TLongArrayList();
        final double[] dist = getDists(mainVec, predicate, queryNorm, orderList);
        final long[] order = orderList.toArray();
        ArrayTools.parallelSort(dist, order);

        int end = Arrays.binarySearch(dist, maxDistance);
        if (end < 0) {
            end = -end - 1;
        }
        return Arrays.stream(order).distinct().limit(end);
    }

    @Override
    public int tupleSize() {
        return TUPLE_SIZE;
    }

    @Override
    public int tablesNumber() {
        return TABLES_NUMBER;
    }

    @Override
    public int maxDiffBits() {
        return MAX_DIFFERENT_BITS;
    }

    private double distance(Vec mainVec, double queryNorm, Vec v) {
        double norm = VecTools.norm(v);
        return norm == 0
                ? Double.POSITIVE_INFINITY
                : (1 - VecTools.multiply(mainVec, v) / norm / queryNorm) / 2;
    }

    @Override
    public int dim() {
        return PlainIndexBuilder.DEFAULT_VEC_SIZE;
    }
}
