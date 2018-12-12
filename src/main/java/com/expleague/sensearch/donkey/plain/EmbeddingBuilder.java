package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.google.common.primitives.Longs;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongObjectMap;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Random;
import java.util.function.ToLongFunction;

import gnu.trove.map.hash.TLongObjectHashMap;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

public class EmbeddingBuilder {

  public static final String VECS_ROOT = "vecs";
  public static final String LSH_ROOT = "lsh";
  public static final String RAND_VECS = "rand";
  public static final int TABLES_NUMBER = 10;
  public static final int TUPLE_SIZE = 32;
  private static final int MAX_BATCH_SIZE = 1_000_000;
  private static final double MIN_COORD_VAL = -1.;
  private static final double MAX_COORD_VAL = 1.;

  private static final Options DB_OPTIONS =
          new Options()
                  .createIfMissing(true)
                  .errorIfExists(true)
                  .compressionType(CompressionType.SNAPPY);

  private static final WriteOptions WRITE_OPTIONS = new WriteOptions().sync(true);
  // .snapshot(false);

  private DB vecDB;
  private WriteBatch batch = null;
  private int batchSize = 0;

  private TLongObjectMap<TLongList> tables = new TLongObjectHashMap<>();
  private DB tablesDB;
  private ToLongFunction<Vec>[] hashFuncs;

  public EmbeddingBuilder(Path embeddingPath) throws IOException {
    vecDB = JniDBFactory.factory.open(embeddingPath.resolve(VECS_ROOT).toFile(), DB_OPTIONS);
    tablesDB = JniDBFactory.factory.open(embeddingPath.resolve(LSH_ROOT).toFile(), DB_OPTIONS);

    hashFuncs = new ToLongFunction[TABLES_NUMBER];

    try (Writer output =
                 new OutputStreamWriter(new FileOutputStream(embeddingPath.resolve(RAND_VECS).toFile()))) {
      Random random = new Random();
      for (int i = 0; i < hashFuncs.length; i++) {

        Vec[] randVecs = new Vec[TUPLE_SIZE];
        for (int j = 0; j < randVecs.length; j++) {
          double[] randCoords = new double[PlainIndexBuilder.DEFAULT_VEC_SIZE];
          for (int k = 0; k < randCoords.length; k++) {
            randCoords[k] = MIN_COORD_VAL + (MAX_COORD_VAL - MIN_COORD_VAL) * random.nextDouble();
            output.write(randCoords[k] + (k < randCoords.length - 1 ? " " : ""));
          }
          randVecs[j] = new ArrayVec(randCoords);
          output.write("\n");
        }

        final int hashNum = i;
        hashFuncs[i] =
                (vec) -> {
                  boolean[] mask = new boolean[TUPLE_SIZE];
                  for (int j = 0; j < mask.length; j++) {
                    mask[j] = VecTools.multiply(vec, randVecs[j]) >= 0;
                  }

                  long hash = (((long) hashNum) << ((long) TUPLE_SIZE));
                  for (int j = 0; j < mask.length; j++) {
                    if (mask[j]) {
                      hash += (1L << ((long) j));
                    }
                  }

                  return hash;
                };
      }
    }
  }

  private void addToTables(long id, Vec vec) {
    for (ToLongFunction<Vec> hashFunc : hashFuncs) {
      long bucketIndex = hashFunc.applyAsLong(vec);
      TLongList bucketEntry = tables.get(bucketIndex);
      if (bucketEntry == null) {
        bucketEntry = new TLongArrayList();
        tables.put(bucketIndex, bucketEntry);
      }
      bucketEntry.add(id);
    }
  }

  private void check(DB db) {
    if (batch == null) {
      batch = db.createWriteBatch();
    }
    if (batchSize > MAX_BATCH_SIZE) {
      db.write(batch, WRITE_OPTIONS);
      batchSize = 0;
      batch = db.createWriteBatch();
    }
  }

  public void add(long id, Vec vec) {
    addToTables(id, vec);
    check(vecDB);
    batch.put(Longs.toByteArray(id), ByteTools.toBytes(vec));
    batchSize++;
  }

  public void addAll(TLongObjectMap<Vec> vecs) {
    vecs.forEachEntry(
            (id, vec) -> {
              add(id, vec);
              return true;
            });
  }

  private void addToTablesDB(long bucket, long[] ids) {
    check(tablesDB);
    batch.put(Longs.toByteArray(bucket), ByteTools.toBytes(ids));
    batchSize++;
  }

  public void build() throws IOException {
    if (batchSize > 0) {
      vecDB.write(batch, WRITE_OPTIONS);
      batchSize = 0;
      batch = null;
    }
    vecDB.close();

    tables.forEachEntry((bucket, entry) -> {
      addToTablesDB(bucket, entry.toArray());
      return true;
    });
    if (batchSize > 0) {
      tablesDB.write(batch, WRITE_OPTIONS);
      batchSize = 0;
      batch = null;
    }
    tablesDB.close();
  }
}
