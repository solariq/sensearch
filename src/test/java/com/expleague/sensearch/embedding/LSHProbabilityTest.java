//package com.expleague.sensearch.embedding;
//import com.expleague.commons.math.vectors.Vec;
//import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
//import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
//import com.expleague.sensearch.index.Embedding;
//import com.expleague.sensearch.index.plain.EmbeddingImpl;
//import com.expleague.sensearch.index.plain.PlainIndex;
//import com.expleague.sensearch.utils.TestConfigImpl;
//import gnu.trove.set.TLongSet;
//import gnu.trove.set.hash.TLongHashSet;
//import org.fusesource.leveldbjni.JniDBFactory;
//import org.iq80.leveldb.DB;
//import org.iq80.leveldb.Options;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import java.io.IOException;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.text.DecimalFormat;
//import java.util.*;
//import java.util.function.LongPredicate;
//public class LSHProbabilityTest {
//    private static final double MIN_COORD_VAL = -1.;
//    private static final double MAX_COORD_VAL = 1.;
//    private static final double D1 = Math.PI / 3.;
//    private static final double D2 = Math.PI / 2.;
//    private static final int RAND_VECS_NUMBER = 10;
//    private static final LongPredicate PREDICATE = id -> id > 0;
//    private static Vec[] randVecs;
//    @BeforeClass
//    public static void initRandVecs() {
//        randVecs = new Vec[RAND_VECS_NUMBER];
//        Random random = new Random();
//        for (int j = 0; j < randVecs.length; j++) {
//            double[] randCoords = new double[PlainIndexBuilder.DEFAULT_VEC_SIZE];
//            for (int k = 0; k < randCoords.length; k++) {
//                randCoords[k] = MIN_COORD_VAL + (MAX_COORD_VAL - MIN_COORD_VAL) * random.nextDouble();
//            }
//            randVecs[j] = new ArrayVec(randCoords);
//        }
//    }
//    private long binomial(int n, int k) {
//        if (k > n - k) {
//            k = n - k;
//        }
//        long b = 1;
//        for (int i = 1, m = n; i <= k; i++, m--) {
//            b = b * m / i;
//        }
//        return b;
//    }
//    private double getTheoreticalP(Embedding embedding, double d) {
//        double binomialP = 0.;
//        double p = 1. - d / Math.PI;
//        double q = 1. - p;
//        for (int diffBits = 0; diffBits <= embedding.maxDiffBits(); diffBits++) {
//            binomialP += ((double) binomial(embedding.tupleSize(), embedding.tupleSize() - diffBits))
//                    * Math.pow(p, embedding.tupleSize() - diffBits)
//                    * Math.pow(q, diffBits);
//        }
//        return 1. - Math.pow(1. - binomialP, embedding.tablesNumber());
//    }
//    private double getPracticalP1(Embedding embedding, long[] lshIds, Vec mainVec, double r1) {
//        long[] necessaryIds = embedding.nearest(mainVec, r1, PREDICATE).toArray();
//        TLongSet intersection = new TLongHashSet(lshIds);
//        intersection.retainAll(necessaryIds);
//        return ((double) intersection.size()) / ((double) necessaryIds.length);
//    }
//    private double getPracticalP2(Embedding embedding, long[] lshIds, Vec mainVec, double r2) {
//        TLongSet needlessIds = new TLongHashSet(embedding.nearest(mainVec, PREDICATE).toArray());
//        needlessIds.removeAll(embedding.nearest(mainVec, r2, PREDICATE).toArray());
//        TLongSet intersection = new TLongHashSet(lshIds);
//        intersection.retainAll(needlessIds);
//        return ((double) intersection.size()) / ((double) needlessIds.size());
//    }
//    private void baseTest(Embedding embedding) {
//        double pracP1 = 0.;
//        double pracP2 = 0.;
//        double time = 0.;
//        int cnt = 0;
//        for (Vec vec : randVecs) {
//            embedding.setLSHFlag(true);
//            long startTime = System.nanoTime();
//            long[] res = embedding.nearest(vec, PREDICATE).toArray();
//            time += (System.nanoTime() - startTime) / 1e9;
//            embedding.setLSHFlag(false);
//            pracP1 += getPracticalP1(embedding, res, vec, Math.cos(D1));
//            pracP2 += getPracticalP2(embedding, res, vec, Math.cos(D2));
//            System.out.println("randVecs done: " + ++cnt);
//        }
//        pracP1 /= ((double) randVecs.length);
//        pracP2 /= ((double) randVecs.length);
//        time /= ((double) randVecs.length);
//        double theorP1 = getTheoreticalP(embedding, D1);
//        double theorP2 = getTheoreticalP(embedding, D2);
//        String pattern = "#0.0000000000";
//        DecimalFormat decimalFormat = new DecimalFormat(pattern);
//        System.out.println(
//            "theorP1: " + decimalFormat.format(theorP1) + "\n"
//            + "pracP1: " + decimalFormat.format(pracP1) + "\n"
//            + "theorP2: " + decimalFormat.format(theorP2) + "\n"
//            + "pracP2: " + decimalFormat.format(pracP2) + "\n"
//            + "time: " + decimalFormat.format(time) + "\n"
//        );
//    }
//    @Test
//    public void test() throws IOException {
//        Path embeddingPath = Paths.get("../WikiDocs/IndexTmp/embedding");
//        long cacheSize = 16 * (1 << 20);
//        Options dbOptions = new Options().cacheSize(cacheSize);
//        DB vecDB = JniDBFactory.factory.open(embeddingPath.resolve("vecs").toFile(), dbOptions);
//        DB tablesDB = JniDBFactory.factory.open(embeddingPath.resolve("lsh").toFile(), dbOptions);
//        Embedding embedding = new EmbeddingImpl(new TestConfigImpl(), vecDB, tablesDB, embeddingPath);
//        baseTest(embedding);
//    }
//}
