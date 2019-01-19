package com.expleague.sensearch.donkey.plain;

import static com.expleague.sensearch.donkey.plain.PlainIndexBuilder.DEFAULT_VEC_SIZE;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.embedding.Embedding;
import com.expleague.sensearch.core.Tokenizer;
import com.google.common.primitives.Longs;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.ToLongFunction;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

public class EmbeddingBuilder implements AutoCloseable {
  public static final String RAND_VECS = "rand";
  public static final int TABLES_NUMBER = 10;
  public static final int TUPLE_SIZE = 20;
  private static final int MAX_BATCH_SIZE = 1 << 14;
  private static final double MIN_COORD_VAL = -1.;
  private static final double MAX_COORD_VAL = 1.;
  private static final WriteOptions WRITE_OPTIONS = new WriteOptions().sync(true);

  private final Tokenizer tokenizer;
  private final Embedding<CharSeq> jmllEmbedding;
  private final IdGenerator idGenerator;
  // .snapshot(false);

  private DB vecDB;
  private WriteBatch batch = null;
  private int batchSize = 0;

  private TLongObjectMap<TLongSet> tables = new TLongObjectHashMap<>();
  private DB tablesDB;
  private ToLongFunction<Vec>[] hashFuncs;

  public EmbeddingBuilder(
      DB vecDb,
      DB tablesD,
      Path embeddingPath,
      Embedding<CharSeq> jmllEmbedding,
      Tokenizer tokenizer,
      IdGenerator idGenerator) {
    this.jmllEmbedding = jmllEmbedding;
    this.tokenizer = tokenizer;
    this.idGenerator = idGenerator;

    this.vecDB = vecDb;
    this.tablesDB = tablesD;

    hashFuncs = new ToLongFunction[TABLES_NUMBER];

    try (Writer output =
        new OutputStreamWriter(new FileOutputStream(embeddingPath.resolve(RAND_VECS).toFile()))) {
      Random random = new Random();
      for (int i = 0; i < hashFuncs.length; i++) {

        Vec[] randVecs = new Vec[TUPLE_SIZE];
        for (int j = 0; j < randVecs.length; j++) {
          double[] randCoords = new double[DEFAULT_VEC_SIZE];
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
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void close() throws IOException {
    if (batchSize > 0) {
      vecDB.write(batch, WRITE_OPTIONS);
      batchSize = 0;
      batch = null;
    }

    tables.forEachEntry(
        (bucket, entry) -> {
          addToTablesDB(bucket, entry.toArray());
          return true;
        });

    if (batchSize > 0) {
      tablesDB.write(batch, WRITE_OPTIONS);
      batchSize = 0;
      batch = null;
    }

    tablesDB.close();
    vecDB.close();
  }

  private void addToTables(long id, Vec vec) {
    for (ToLongFunction<Vec> hashFunc : hashFuncs) {
      long bucketIndex = hashFunc.applyAsLong(vec);
      TLongSet bucketEntry = tables.get(bucketIndex);
      if (bucketEntry == null) {
        bucketEntry = new TLongHashSet();
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
      try {
        batch.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      batch = db.createWriteBatch();
    }
  }

  private long curPageId = 0;
  private List<Vec> curPageVecs = new ArrayList<>();

  public void startPage(long pageId) {
    if (curPageId != 0) {
      throw new IllegalStateException(
          "Invalid call to startPage(): already processing page [" + curPageId + "]");
    }
    curPageId = pageId;
    curPageVecs = new ArrayList<>();
  }

  public void add(String text) {
    tokenizer
        .toParagraphs(text)
        .map(this::toVector)
        .forEach(
            vec -> {
              addToTables(curPageId, vec);
              curPageVecs.add(vec);
            });

    tokenizer.toWords(text).map(word -> word.toString().toLowerCase()).forEach(word -> {
      long id = idGenerator.termId(word);

      Vec vec = jmllEmbedding.apply(CharSeq.compact(word));
      if (vec != null) {
        check(vecDB);
        batch.put(Longs.toByteArray(id), ByteTools.toBytes(vec));
        batchSize++;
        addToTables(id, vec);
      }

    });
  }

  public void endPage() {

    check(vecDB);
    batch.put(Longs.toByteArray(curPageId), ByteTools.toBytes(curPageVecs));
    batchSize++;

    curPageId = 0;
  }

  private void addToTablesDB(long bucket, long[] ids) {
    check(tablesDB);
    batch.put(Longs.toByteArray(bucket), ByteTools.toBytes(ids));
    batchSize++;
  }

  private Vec toVector(CharSequence text) {
    Vec[] vectors =
        tokenizer
            .parseTextToWords(text)
            .map(CharSeq::intern)
            .map(jmllEmbedding)
            .filter(Objects::nonNull)
            .toArray(Vec[]::new);

    if (vectors.length == 0) {
      return new ArrayVec(DEFAULT_VEC_SIZE);
    }

    ArrayVec mean = new ArrayVec(DEFAULT_VEC_SIZE);
    for (Vec vec : vectors) {
      VecTools.append(mean, vec);
    }
    return VecTools.scale(mean, 1.0 / vectors.length);
  }
}
