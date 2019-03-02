package com.expleague.sensearch.embedding;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.nn.NearestNeighbourIndex;
import com.expleague.commons.random.FastRandom;
import com.expleague.commons.util.ArrayTools;
import com.expleague.sensearch.donkey.plain.ByteTools;
import com.expleague.sensearch.index.plain.QuantLSHCosIndexDB;
import com.google.common.primitives.Longs;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.LongStream;

public class QuantLSHCosIndexDBParametersTest {
    private static final int DIM = 130;

    private static final int MIN_QUANT_DIM = 65;
    private static final int MAX_QUANT_DIM = 65;
    private static final int QUANT_DIM_STEP = 5;

    private static final int MIN_SKETCH_BITS_PER_QUANT = 96;
    private static final int MAX_SKETCH_BITS_PER_QUANT = 96;
    private static final int SKETCH_BITS_PER_QUANT_STEP = 2;

    private static final int MIN_BATCH_SIZE = 1500;
    private static final int MAX_BATCH_SIZE = 1500;
    private static final int BATCH_SIZE_STEP = 500;

    private static final int TIME_ATTEMPTS = 100;
    private static final int QUALITY_ATTEMPTS = 10;
    private static final int NUM = 100;

    private static final long CACHE_SIZE = 16 * (1 << 20);
    private static final Options DB_OPTIONS = new Options().cacheSize(CACHE_SIZE);

    private LongStream naiveNearest(long[] ids, DB vecDB, Vec qVec) {
        double[] dists = LongStream.of(ids).mapToDouble(id -> {
            Vec curVec = ByteTools.toVec(vecDB.get(Longs.toByteArray(id)));
            return 1. - VecTools.cosine(qVec, curVec);
        }).toArray();
        ArrayTools.parallelSort(dists, ids);
        return LongStream.of(ids);
    }

    @Ignore
    @Test
    public void parametersTest() throws IOException {
        Path embeddingPath = Paths.get("../WikiDocs/IndexTmp/embedding");
        DB vecDB = JniDBFactory.factory.open(embeddingPath.resolve("vecs").toFile(), DB_OPTIONS);

        TLongList idList = new TLongArrayList();
        try (DBIterator iterator = vecDB.iterator();) {
            for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                byte[] key = iterator.peekNext().getKey();
                long id = Longs.fromByteArray(key);
                if (id < Long.MAX_VALUE - 3) {
                    idList.add(id);
                }
            }
        }
        long[] allIds = idList.toArray();
        FastRandom rng = new FastRandom();

        try (PrintWriter out = new PrintWriter(new FileOutputStream(new File("ParamAnalysis.txt")), true)) {
            for (int quantDim = MIN_QUANT_DIM; quantDim <= MAX_QUANT_DIM; quantDim += QUANT_DIM_STEP) {
                for (int sketchBitsPerQuant = MIN_SKETCH_BITS_PER_QUANT; sketchBitsPerQuant <= MAX_SKETCH_BITS_PER_QUANT; sketchBitsPerQuant += SKETCH_BITS_PER_QUANT_STEP) {
                    for (int batchSize = MIN_BATCH_SIZE; batchSize <= MAX_BATCH_SIZE; batchSize += BATCH_SIZE_STEP) {

                        QuantLSHCosIndexDB nnIdx = new QuantLSHCosIndexDB(rng, DIM, quantDim, sketchBitsPerQuant, batchSize, vecDB);
                        try (DBIterator iterator = vecDB.iterator();) {
                            for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                                byte[] key = iterator.peekNext().getKey();
                                byte[] value = iterator.peekNext().getValue();
                                long id = Longs.fromByteArray(key);
                                Vec vec = ByteTools.toVec(value);
                                if (id < Long.MAX_VALUE - 3) {
                                    nnIdx.append(id, vec);
                                }
                            }
                        } catch (IOException e){
                            throw new RuntimeException(e);
                        }

                        double result = 0;
                        double timeLSH = 0;
                        double timeNaive = 0;

                        try {
                            for (int i = 0; i < QUALITY_ATTEMPTS; i++) {
                                long id = allIds[rng.nextInt(allIds.length)];
                                Vec qVec = ByteTools.toVec(vecDB.get(Longs.toByteArray(id)));

                                TLongSet lshResult = new TLongHashSet(nnIdx.nearest(qVec).mapToLong(NearestNeighbourIndex.Entry::id).filter(idd -> idd > 0).limit(NUM).toArray());

                                long start = System.currentTimeMillis();
                                TLongSet trueResult = new TLongHashSet(naiveNearest(allIds, vecDB, qVec).filter(idd -> idd > 0).limit(NUM).toArray());
                                long end = System.currentTimeMillis();
                                timeNaive += (end - start) / 1e3;

                                lshResult.retainAll(trueResult);
                                result += lshResult.size() / ((double) trueResult.size());
                            }

                            for (int i = 0; i < TIME_ATTEMPTS; i++) {
                                long id = allIds[rng.nextInt(allIds.length)];
                                Vec qVec = ByteTools.toVec(vecDB.get(Longs.toByteArray(id)));

                                long start = System.currentTimeMillis();
                                nnIdx.nearest(qVec).mapToLong(NearestNeighbourIndex.Entry::id).filter(idd -> idd > 0).limit(NUM);
                                long end = System.currentTimeMillis();

                                timeLSH += (end - start) / 1e3;

                            }
                        } catch (Exception e) {
                            System.err.println("quantDim: " + quantDim
                                    + "\nsketchBitsPerQuant: " + sketchBitsPerQuant
                                    + "\nbatchSize: " + batchSize
                            );
                            e.printStackTrace();
                            continue;
                        }

                        result /= QUALITY_ATTEMPTS;
                        timeLSH /= TIME_ATTEMPTS;
                        timeNaive /= QUALITY_ATTEMPTS;
                        out.println(
                                "quantDim: " + quantDim
                                + "\nsketchBitsPerQuant: " + sketchBitsPerQuant
                                + "\nbatchSize: " + batchSize
                                + "\naverage result: " + result
                                + "\naverage time LSH: " + timeLSH
                                + "\naverage time naive: " + timeNaive + "\n");
                    }
                }
            }
        } catch (FileNotFoundException ignored) {}
    }
}