package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics.TermFrequency;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.primitives.Longs;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

public class StatisticsBuilder {

  private static final long DEFAULT_CACHE_SIZE = 16 * (1 << 20); // 16 MB

  private static final Options DEFAULT_DB_OPTIONS = new Options()
      .cacheSize(DEFAULT_CACHE_SIZE)
      .createIfMissing(true)
      .errorIfExists(true)
      .compressionType(CompressionType.SNAPPY);

  private static final WriteOptions DEFAULT_WRITE_OPTIONS = new WriteOptions()
      .sync(true)
      .snapshot(false);

  private static final int MOST_FREQUENT_BIGRAMS_COUNT = 10;

  private final TLongLongMap wordFrequencyMap = new TLongLongHashMap();
  private final TLongIntMap documentFrequencyMap = new TLongIntHashMap();
  private final TLongObjectMap<TLongIntMap> largeBigramsMap = new TLongObjectHashMap<>();

  private final DB statisticsDb;

  StatisticsBuilder(Path statisticsRoot) throws IOException {
    statisticsDb = JniDBFactory.factory.open(statisticsRoot.toFile(), DEFAULT_DB_OPTIONS);
  }

  @VisibleForTesting
  static Iterable<TermStatistics.TermFrequency> mostFrequentBigrams(TLongIntMap neighbours,
      int keep) {
    if (neighbours == null || neighbours.isEmpty()) {
      return new LinkedList<>();
    }

    MinMaxPriorityQueue<IdFrequencyPair> neighboursHeap = MinMaxPriorityQueue
        .orderedBy(Comparator.comparingInt(IdFrequencyPair::frequency).reversed())
        .maximumSize(keep)
        .expectedSize(keep)
        .create();

    neighbours.forEachEntry(
        (neighId, freq) -> {
          neighboursHeap.add(new IdFrequencyPair(neighId, freq));
          return true;
        }
    );

    final TermFrequency.Builder tfBuilder = TermFrequency.newBuilder();
    final List<TermStatistics.TermFrequency> termFrequencies = new LinkedList<>();
    neighboursHeap.forEach(
        p -> termFrequencies.add(tfBuilder
            .setTermFrequency(p.frequency())
            .setTermId(p.termId())
            .build()
        )
    );

    return termFrequencies;
  }

  void enrich(TLongIntMap pageWiseTf, TLongObjectMap<TLongIntMap> pageWiseBigramTf) {
    pageWiseTf.forEachEntry(
        (tok, freq) -> {
          wordFrequencyMap.adjustOrPutValue(tok, freq, freq);
          documentFrequencyMap.adjustOrPutValue(tok, 1, 1);
          return true;
        }
    );

    pageWiseBigramTf.forEachEntry(
        (tId, neigh) -> {
          largeBigramsMap.putIfAbsent(tId, new TLongIntHashMap());
          TLongIntMap existingNeighStats = largeBigramsMap.get(tId);
          neigh.forEachEntry(
              (nId, freq) -> {
                existingNeighStats.adjustOrPutValue(nId, freq, freq);
                return true;
              }
          );
          return true;
        }
    );
  }

  void build() throws IOException {
    WriteBatch writeBatch = statisticsDb.createWriteBatch();
    final TermStatistics.Builder tsBuilder = TermStatistics.newBuilder();
    wordFrequencyMap.forEachKey(
        k -> {
          writeBatch.put(
              Longs.toByteArray(k),
              tsBuilder
                  .setTermId(k)
                  .setTermFrequency(wordFrequencyMap.get(k))
                  .setDocuementFrequency(documentFrequencyMap.get(k))
                  .addAllBigramFrequency(mostFrequentBigrams(
                      largeBigramsMap.get(k),
                      MOST_FREQUENT_BIGRAMS_COUNT)
                  )
                  .build()
                  .toByteArray()
          );

          tsBuilder.clear();
          return true;
        }
    );

    statisticsDb.write(writeBatch, DEFAULT_WRITE_OPTIONS);
    statisticsDb.close();
  }

  private static class IdFrequencyPair {

    long termId;
    int frequency;

    IdFrequencyPair(long termId, int frequency) {
      this.termId = termId;
      this.frequency = frequency;
    }

    long termId() {
      return termId;
    }

    int frequency() {
      return frequency;
    }
  }
}
