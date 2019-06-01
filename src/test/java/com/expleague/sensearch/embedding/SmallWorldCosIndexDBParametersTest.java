package com.expleague.sensearch.embedding;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.nn.NearestNeighbourIndex;
import com.expleague.commons.random.FastRandom;
import com.expleague.commons.util.ArrayTools;
import com.expleague.commons.util.Pair;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.core.Annotations.EmbeddingVecsDb;
import com.expleague.sensearch.core.ByteTools;
import com.expleague.sensearch.index.Embedding;
import com.expleague.sensearch.index.plain.SmallWorldCosIndexDB;
import com.google.common.primitives.Longs;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.junit.Ignore;
import org.junit.Test;

public class SmallWorldCosIndexDBParametersTest {

  private static final Path OUTPUT_PATH = Paths.get("ParamAnalisysSmallWorld.txt");
  private static final Path EMBEDDING_PATH = Paths.get("../WikiDocs/IndexTmp/embedding");

  private static final int BATCH_SIZE = 128;

  private static final int DIM = 130;

  private static final int MIN_MAX_NEIGHBOURS = 20;
  private static final int MAX_MAX_NEIGHBOURS = 100;
  private static final int MAX_NEIGHBOURS_STEP = 10;

  private static final int MIN_NUM_SEARCHES = 2;
  private static final int MAX_NUM_SEARCHES = 4;
  private static final int NUM_SEARCHES_STEP = 1;

  private static final int TIME_ATTEMPTS = 100;
  private static final int QUALITY_ATTEMPTS = 10;
  private static final int NUM = 100;

  private static final long CACHE_SIZE = 16 * (1 << 20);
  private static final Options DB_OPTIONS = new Options().cacheSize(CACHE_SIZE);
  private Embedding embedding;

  private LongStream naiveNearest(long[] ids, DB vecDB, Vec qVec) {
    double[] dists = LongStream.of(ids).mapToDouble(id -> {
      Vec curVec = embedding.vec(id);
      return 1. - VecTools.cosine(qVec, curVec);
    }).toArray();
    ArrayTools.parallelSort(dists, ids);
    return LongStream.of(ids);
  }

  @Ignore
  @Test
  public void parametersTest() throws IOException {
//    DB vecDB = JniDBFactory.factory.open(EMBEDDING_PATH.resolve("vecs").toFile(), DB_OPTIONS);
    DB smallWorldDb = JniDBFactory.factory.open(EMBEDDING_PATH.resolve("smallWorld").toFile(), DB_OPTIONS);

    Injector injector = Guice.createInjector(new AppModule());
    embedding = injector.getInstance(Embedding.class);
    DB vecDB = injector.getInstance(Key.get(DB.class, EmbeddingVecsDb.class));

    TLongList idList = new TLongArrayList();
    try (DBIterator iterator = vecDB.iterator();) {
      for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
        byte[] key = iterator.peekNext().getKey();
        long id = Longs.fromByteArray(key);
        if (id < Long.MAX_VALUE - 300) {
          idList.add(id);
        }
      }
    }
    long[] allIds = idList.toArray();
    FastRandom rng = new FastRandom(239);

    try (PrintWriter out = new PrintWriter(new FileOutputStream(OUTPUT_PATH.toFile()), true); BufferedWriter w = Files
        .newBufferedWriter(Paths.get("data.txt"), StandardCharsets.UTF_8)) {
      for (int maxNeighbours = MIN_MAX_NEIGHBOURS; maxNeighbours <= MAX_MAX_NEIGHBOURS;
          maxNeighbours += MAX_NEIGHBOURS_STEP) {
        for (int numSearches = MIN_NUM_SEARCHES; numSearches <= MAX_NUM_SEARCHES; numSearches += NUM_SEARCHES_STEP) {
          SmallWorldCosIndexDB nnIdx = new SmallWorldCosIndexDB(rng, DIM, maxNeighbours, numSearches, embedding,
              smallWorldDb);
          List<Pair<Long, Vec>> batch = new ArrayList<>();
          try (DBIterator iterator = vecDB.iterator()) {
            int cnt = 0;
            long start = System.nanoTime();
            for (iterator.seekToFirst(); iterator.hasNext(); iterator.next(), cnt++) {
              byte[] key = iterator.peekNext().getKey();
              byte[] value = iterator.peekNext().getValue();
              long id = Longs.fromByteArray(key);
              Vec vec = ByteTools.toVec(value);
              if (id < Long.MAX_VALUE - 200) {
//                batch.add(Pair.create(id, vec));
              }
              if (batch.size() >= BATCH_SIZE || !iterator.hasNext()) {
                nnIdx.appendBatch(
                    batch.stream().mapToLong(Pair::getFirst).toArray(),
                    batch.stream().map(Pair::getSecond).toArray(Vec[]::new));
                batch.clear();
              }
              w.write(id + " " + vec.toString() + "\n");
//              if (cnt > 500_000) {
//                System.out.println(cnt + " elements added in " + (System.nanoTime() - start) / 1e9 + "s");
//                break;
//              }
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }

          System.out.println("kek");
          double result = 0;
          double timeLSH = 0;
          double timeNaive = 0;

          w.write("\n");
          try {
            for (int i = 0; i < QUALITY_ATTEMPTS; i++) {
              long id = allIds[rng.nextInt(allIds.length)];
              Vec qVec = ByteTools.toVec(vecDB.get(Longs.toByteArray(id)));

              TLongSet lshResult = new TLongHashSet(
//                  nnIdx.nearest(qVec).mapToLong(NearestNeighbourIndex.Entry::id).filter(idd -> idd > 0).limit(NUM)
//                      .toArray()
              );

              long start = System.currentTimeMillis();
              TLongSet trueResult = new TLongHashSet(
                  naiveNearest(allIds, vecDB, qVec).filter(idd -> idd > 0).limit(NUM).toArray());
              w.write(id + "\n");
              w.write(
                  Arrays.stream(trueResult.toArray()).boxed().map(Object::toString).collect(Collectors.joining(" ")));
              w.write("\n");
              long end = System.currentTimeMillis();
              timeNaive += (end - start) / 1e3;

              lshResult.retainAll(trueResult);
              result += lshResult.size() / ((double) trueResult.size());
            }

            for (int i = 0; i < TIME_ATTEMPTS; i++) {
              long id = allIds[rng.nextInt(allIds.length)];
              Vec qVec = ByteTools.toVec(vecDB.get(Longs.toByteArray(id)));

              long start = System.currentTimeMillis();
              nnIdx.nearest(qVec).mapToLong(NearestNeighbourIndex.Entry::id).filter(idd -> idd > 0).limit(NUM)
                  .toArray();
              long end = System.currentTimeMillis();

              timeLSH += (end - start) / 1e3;

            }
          } catch (Exception e) {
            System.err.println("maxNeighbours: " + maxNeighbours
                + "\nnumSearches: " + numSearches
            );
            e.printStackTrace();
            continue;
          }

          System.exit(0);
          result /= QUALITY_ATTEMPTS;
          timeLSH /= TIME_ATTEMPTS;
          timeNaive /= QUALITY_ATTEMPTS;
          out.println(
              "maxNeighbours: " + maxNeighbours
                  + "\nnumSearches: " + numSearches
                  + "\naverage result: " + result
                  + "\naverage time LSH: " + timeLSH
                  + "\naverage time naive: " + timeNaive + "\n");
        }
      }
    }
  }
}