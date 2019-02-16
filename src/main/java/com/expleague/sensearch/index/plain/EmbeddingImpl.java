package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.nn.NearestNeighbourIndex;
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
    private long[] allIds;

    private boolean lshFlag;

    private QuantLSHCosIndexDB nnIdx;

    @Inject
    public EmbeddingImpl(
            Config config,
            @EmbeddingVecsDb DB vecDb) {
        lshFlag = config.getLshNearestFlag();
        nnIdx = QuantLSHCosIndexDB.load(vecDb);
    }

    @Override
    public Vec vec(long id) {
        return nnIdx.get(id);
    }

    private long[] lshNearest(Vec mainVec) {
        return nnIdx.nearest(mainVec).mapToLong(NearestNeighbourIndex.Entry::id).toArray();
    }

    private double[] getDists(Vec mainVec, LongPredicate predicate, double queryNorm, TLongList orderList) {
        long[] ids = lshFlag ? lshNearest(mainVec) : allIds;
        return LongStream.of(ids)
                .filter(predicate)
                .peek(orderList::add)
                .mapToDouble(id -> distance(mainVec, queryNorm, vec(id)))
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
