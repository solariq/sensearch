package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.nn.NearestNeighbourIndex;
import com.expleague.commons.util.ArrayTools;
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
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

public class EmbeddingImpl implements Embedding {
    public long[] allIds;
    private boolean lshFlag;
    private QuantLSHCosIndexDB nnIdx;

    @Inject
    public EmbeddingImpl(@UseLshFlag boolean useLshFlag, @EmbeddingVecsDb DB vecDb) {
        lshFlag = useLshFlag;
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

    @Override
    public void setLSHFlag(boolean value) {
        lshFlag = value;
    }

    @Override
    public Stream<Candidate> nearest(Vec qVec, LongPredicate predicate) {
        return baseNearest(qVec, predicate, Integer.MAX_VALUE);
    }

    @Override
    public Stream<Candidate> nearest(Vec qVec, LongPredicate predicate, int numberOfNeighbors) {
        return baseNearest(qVec, predicate, numberOfNeighbors);
    }

    @Override
    public Stream<Candidate> nearest(Vec qVec, LongPredicate predicate, double maxDistance) {
        return baseNearest(qVec, predicate, maxDistance);
    }

    //todo: replace object either
    private Stream<Candidate> baseNearest(Vec qVec, LongPredicate predicate, Object either) {
        final double qNorm = VecTools.norm(qVec);
        if (qNorm == 0) {
            return Stream.empty();
        }
        final int num = either instanceof Integer ? (int)either : Integer.MAX_VALUE;
        final double distance = either instanceof Double ? (double)either : Double.POSITIVE_INFINITY;

        if (!lshFlag) {
            final long[] ids = LongStream.of(allIds)
                .filter(predicate)
                .distinct()
                .toArray();
            final double[] dists = LongStream.of(ids)
                .mapToDouble(id -> distance(qVec, qNorm, vec(id)))
                .toArray();
            ArrayTools.parallelSort(dists, ids);
            int count = Math.min(ids.length, num < ids.length ? num : Arrays.binarySearch(dists, distance));

            List<Candidate> candidates = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                candidates.add(new Candidate(ids[i], dists[i]));
            }
            return candidates.stream();
        }
        final Spliterator<NearestNeighbourIndex.Entry> spliterator = nnIdx.nearest(qVec).spliterator();
        return StreamSupport.stream(new Spliterators.AbstractSpliterator<Candidate>(num, Spliterator.IMMUTABLE | Spliterator.NONNULL) {
            boolean eos = false;
            int count = 0;
            @Override
            public boolean tryAdvance(Consumer<? super Candidate> action) {
                if (eos)
                    return false;
                spliterator.tryAdvance(entry -> {
                  if (!predicate.test(entry.id())) {
                    return;
                  }
                    if (++count > num)
                        eos = true;
                    if (entry.distance() > distance)
                        eos = true;
                    if (!eos)
                        action.accept(new Candidate(entry.id(), entry.distance()));
                });
                return !eos;
            }
        }, false);

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
