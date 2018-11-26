package com.expleague.sensearch.embedding;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.sensearch.donkey.plain.EmbeddingBuilder;
import com.expleague.sensearch.index.Embedding;
import com.expleague.sensearch.index.plain.EmbeddingImpl;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class EmbeddingTest {

    private static final String EMBEDDING_TEST_ROOT = "embeddingTest";
    private static final int MAIN_VEC_NUMBER = 10;
    private static final int NEIGHBORS_NUMBER = 5;
    private static final int MAX_ID = MAIN_VEC_NUMBER * (NEIGHBORS_NUMBER + 1);
    private static final int DIFFERENT_COORDS_NUMBER = 10;
    private static final int VEC_SIZE = 50;
    private static final double EPS = 1e-2;

    private static Random random = new Random();
    private static TLongObjectMap<Vec> idVecMap;
    private static TLongSet[] neighbors;
    private static Path embeddingTestPath = Paths.get(EMBEDDING_TEST_ROOT);
    private static Embedding embedding;

    //TODO: Remove builder or not?
    @BeforeClass
    public static void init() throws IOException {
        Files.createDirectories(embeddingTestPath);

        EmbeddingBuilder builder = new EmbeddingBuilder(embeddingTestPath);
        idVecMap = new TLongObjectHashMap<>();
        neighbors = new TLongHashSet[MAIN_VEC_NUMBER];

        for (int mainId = 0; mainId < MAIN_VEC_NUMBER; mainId++) {
            double[] randCoords = new double[VEC_SIZE];
            for (int j = 0; j < randCoords.length; j++) {
                randCoords[j] = random.nextDouble();
            }
            Vec mainVec = new ArrayVec(randCoords);
            idVecMap.put((long) mainId, mainVec);

            neighbors[mainId] = new TLongHashSet();
            for (int nIndex = 0; nIndex < NEIGHBORS_NUMBER; nIndex++) {
                long neighborId = MAIN_VEC_NUMBER + (long) mainId * NEIGHBORS_NUMBER + nIndex;
                Vec neighborVec = new ArrayVec(mainVec.toArray());
                for (int j = 0; j < DIFFERENT_COORDS_NUMBER; j++) {
                    int pos = random.nextInt(VEC_SIZE);
                    neighborVec.set(pos, neighborVec.get(pos) + (random.nextBoolean() ? EPS : -EPS));
                }

                idVecMap.put(neighborId, neighborVec);
                neighbors[mainId].add(neighborId);
            }
        }

        builder.addAll(idVecMap);
        builder.build();
        embedding = new EmbeddingImpl(embeddingTestPath);
    }

    @Test
    public void getVecTest() throws Exception {
        int idsNumber = 10;
        TLongList ids = new TLongArrayList();
        for (int i = 0; i < idsNumber; i++) {
            ids.add(random.nextInt(MAX_ID));
        }
        Assert.assertNotNull(embedding.getVec(ids));

        ids.clear();
        for (int i = 0; i < idsNumber; i++) {
            ids.add(MAX_ID + random.nextInt(MAX_ID));
        }
        Assert.assertNull(embedding.getVec(ids));
        FileUtils.deleteDirectory(embeddingTestPath.toFile());
    }

    @Test
    public void getNearestTest() throws Exception {
        for (int i = 0; i < MAIN_VEC_NUMBER; i++) {
            TLongSet curNeighbors = neighbors[i];
            Assert.assertTrue(
                    embedding.getNearest(
                            idVecMap.get(i),
                            NEIGHBORS_NUMBER
                    ).anyMatch(curNeighbors::contains)
            );
        }
        FileUtils.deleteDirectory(embeddingTestPath.toFile());
    }
}
