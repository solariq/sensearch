package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.nn.NearestNeighbourIndex;
import com.expleague.commons.util.ArrayTools;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Annotations.EmbeddingVecsDb;
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
import java.util.function.LongPredicate;
import java.util.stream.LongStream;

import java.util.stream.Stream;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

public class EmbeddingImpl implements Embedding {
    public long[] allIds;
    private boolean lshFlag;
    private QuantLSHCosIndexDB nnIdx;

    @Inject
    public EmbeddingImpl(
            Config config,
            @EmbeddingVecsDb DB vecDb) {
        lshFlag = config.getLshNearestFlag();
        nnIdx = QuantLSHCosIndexDB.load(vecDb);

        TLongList idList = new TLongArrayList();
        try (DBIterator iterator = vecDb.iterator();) {
            for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                byte[] key = iterator.peekNext().getKey();
                long id = Longs.fromByteArray(key);
                if (id < Long.MAX_VALUE - 3) {
                    idList.add(id);
                }
            }
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        allIds = idList.toArray();
    }

    @Override
    public Vec vec(long id) {
        return nnIdx.get(id);
    }

    private long[] lshNearest(Vec qVec) {
        return nnIdx.nearest(qVec).mapToLong(NearestNeighbourIndex.Entry::id).toArray();
    }

    private double[] getDists(Vec qVec, LongPredicate predicate, double qNorm, TLongList idList) {
        long[] ids = lshFlag ? lshNearest(qVec) : allIds;
        return LongStream.of(ids)
                .filter(predicate)
                .peek(idList::add)
                .mapToDouble(id -> distance(qVec, qNorm, vec(id)))
                .toArray();
    }

    @Override
    public void setLSHFlag(boolean value) {
        lshFlag = value;
    }

    //TODO: remove copypaste
    @Override
    public Stream<Candidate> nearest(Vec qVec, LongPredicate predicate) {
        final double qNorm = VecTools.norm(qVec);
        if (qNorm == 0) {
            return Stream.empty();
        }
        TLongList idList = new TLongArrayList();
        double[] dists = getDists(qVec, predicate, qNorm, idList);
        long[] ids = idList.toArray();

        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            candidates.add(new Candidate(ids[i], dists[i]));
        }
        return candidates.stream();
    }

    //TODO: remove copypaste
    @Override
    public Stream<Candidate> nearest(Vec qVec, int numberOfNeighbors, LongPredicate predicate) {
        final double qNorm = VecTools.norm(qVec);
        if (qNorm == 0) {
            return Stream.empty();
        }

        TLongList idList = new TLongArrayList();
        final double[] dists = getDists(qVec, predicate, qNorm, idList);
        final long[] ids = idList.toArray();
        ArrayTools.parallelSort(dists, ids);

        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < numberOfNeighbors; i++) {
            candidates.add(new Candidate(ids[i], dists[i]));
        }

        return candidates.stream();
    }

    //TODO: remove copypaste
    @Override
    public Stream<Candidate> nearest(Vec qVec, double maxDistance, LongPredicate predicate) {
        final double qNorm = VecTools.norm(qVec);
        if (qNorm == 0) {
            return Stream.empty();
        }

        TLongList idList = new TLongArrayList();
        final double[] dists = getDists(qVec, predicate, qNorm, idList);
        final long[] ids = idList.toArray();
        ArrayTools.parallelSort(dists, ids);

        int end = Arrays.binarySearch(dists, maxDistance);
        if (end < 0) {
            end = -end - 1;
        }

        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < end; i++) {
            candidates.add(new Candidate(ids[i], dists[i]));
        }
        return candidates.stream();
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
