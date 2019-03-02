package com.expleague.sensearch.embedding;

import com.expleague.commons.random.FastRandom;
import com.expleague.sensearch.index.plain.Candidate;
import com.expleague.sensearch.index.plain.EmbeddingImpl;
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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.LongPredicate;

public class EmbeddingPerformanceTest {
    private static final int ATTEMPTS = 10;
    private static final int NUM = 100;
    private static final LongPredicate IS_TERM = id -> id > 0;

    @Ignore
    @Test
    public void performanceTest() throws IOException {
        Path embeddingPath = Paths.get("/Users/solar/data/search/WikiDocs/IndexTmp/embedding");
        long cacheSize = 16 * (1 << 20);
        Options dbOptions = new Options().cacheSize(cacheSize);
        DB vecDB = JniDBFactory.factory.open(embeddingPath.resolve("vecs").toFile(), dbOptions);

        TLongList idList = new TLongArrayList();
        try (DBIterator iterator = vecDB.iterator();) {
            for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                byte[] key = iterator.peekNext().getKey();
                long id = Longs.fromByteArray(key);
                if (IS_TERM.test(id) && id < Long.MAX_VALUE - 3) {
                    idList.add(id);
                }
            }
        }
        long[] termIds = idList.toArray();

        FastRandom rng = new FastRandom();
        EmbeddingImpl embedding = new EmbeddingImpl(true, vecDB);
        double result = 0;
        double timeLSH = 0;
        double prevTimeLSH = 0;
        double timeNaive = 0;
        System.out.print("Iterations: ");
        for (int i = 0; i < ATTEMPTS; i++) {
            long id = termIds[rng.nextInt(termIds.length)];
            TLongSet lshResult;
            {
                long start = System.currentTimeMillis();
                lshResult = new TLongHashSet(embedding.nearest(embedding.vec(id), IS_TERM, NUM, true).mapToLong(Candidate::getId).toArray());
                long end = System.currentTimeMillis();
                timeLSH += (end - start) / 1e3;
            }

            TLongSet trueResult;
            {
                long start = System.currentTimeMillis();
                trueResult = new TLongHashSet(embedding.nearest(embedding.vec(id), IS_TERM, NUM, false).mapToLong(Candidate::getId).toArray());
                long end = System.currentTimeMillis();
                timeNaive += (end - start) / 1e3;
            }
            lshResult.retainAll(trueResult);

            result += lshResult.size() / ((double) trueResult.size());
            if ((i + 1) % 1 == 0) {
                System.out.println("\rIterations: " + (i + 1) + " mean time: " + ((timeLSH - prevTimeLSH) / 1));
                prevTimeLSH = timeLSH;
            }
        }
        System.out.println();
        result /= ATTEMPTS;
        timeLSH /= ATTEMPTS;
        timeNaive /= ATTEMPTS;
        System.out.println("Average result: " + result + "\nAverage time LSH: " + timeLSH + "\nAverage time naive: " + timeNaive);
    }
}
