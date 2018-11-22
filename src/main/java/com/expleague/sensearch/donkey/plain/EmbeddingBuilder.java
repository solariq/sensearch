package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.google.common.primitives.Longs;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongObjectMap;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.*;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.ToIntFunction;

public class EmbeddingBuilder {
  private static final int MAX_BATCH_SIZE = 100;

  private static final int TABLES_NUMBER = 10;
  private static final int TABLE_SIZE = 10;

  private TLongList[][] tables = new TLongArrayList[TABLES_NUMBER][TABLE_SIZE];
  public static ToIntFunction<Vec>[] hashFuncs;
  static {
    hashFuncs = new ToIntFunction[TABLES_NUMBER];
    //TODO implement
  }

  private static final Options DB_OPTIONS = new Options()
          .createIfMissing(true)
          .errorIfExists(true)
          .compressionType(CompressionType.SNAPPY);

  private static final WriteOptions WRITE_OPTIONS = new WriteOptions()
          .sync(true);
          //.snapshot(false);

  private DB vecDb;
  private WriteBatch batch = null;
  private int batchSize = 0;

  EmbeddingBuilder(Path embeddingPath) throws IOException {
      vecDb = JniDBFactory.factory.open(embeddingPath.toFile(), DB_OPTIONS);
  }

  void add(long id, Vec vec) {
    for (int i = 0; i < tables.length; i++) {
      int bucketIndex = hashFuncs[i].applyAsInt(vec);
      tables[i][bucketIndex].add(id);
    }
    if (batch == null) {
      batch = vecDb.createWriteBatch();
    }
    if (batchSize > MAX_BATCH_SIZE) {
      vecDb.write(batch, WRITE_OPTIONS);
      batchSize = 0;
      batch = vecDb.createWriteBatch();
    }
    batch.put(Longs.toByteArray(id), ByteTools.toBytes(vec));
  }

  void addAll(TLongObjectMap<Vec> vecs) {
    vecs.forEachEntry((id, vec) -> {
      add(id, vec);
      return true;
    });
  }

  void build() throws IOException {
    if (batchSize > 0) {
      vecDb.write(batch, WRITE_OPTIONS);
      batchSize = 0;
      batch = null;
    }
    vecDb.close();
  }
}
