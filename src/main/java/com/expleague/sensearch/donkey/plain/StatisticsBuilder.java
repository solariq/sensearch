package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

class StatisticsBuilder {

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

  private final TIntLongMap wordFrequencyMap = new TIntLongHashMap();
  private final TIntIntMap documentFrequencyMap = new TIntIntHashMap();
  private final TIntObjectMap<TIntIntMap> largeBigramsMap = new TIntObjectHashMap<>();

  private final DB statisticsDb;

  StatisticsBuilder(Path statisticsRoot) throws IOException {
    statisticsDb = JniDBFactory.factory.open(statisticsRoot.toFile(), DEFAULT_DB_OPTIONS);
  }

  void enrich(TIntIntMap pageWiseTf, TIntObjectMap<TIntIntMap> pageWiseBigramTf) {
    pageWiseTf.forEachEntry(
        (tok, freq) -> {
          wordFrequencyMap.adjustOrPutValue(tok, freq, freq);
          documentFrequencyMap.adjustOrPutValue(tok, 1, 1);
          return true;
        }
    );

    pageWiseBigramTf.forEachEntry(
        (tId, neigh) -> {

          largeBigramsMap.putIfAbsent(tId, new TIntIntHashMap());
          TIntIntMap existingNeighStats = largeBigramsMap.get(tId);
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

  private Iterable<TermStatistics.TermFrequency> mostFrequentBigrams(TIntIntMap neighbours) {
    if (neighbours == null || neighbours.isEmpty()) {
      return new LinkedList<>();
    }

    NavigableSet<IdFrequencyPair> sortedNeighbours = new TreeSet<>(
        Comparator.comparingInt(IdFrequencyPair::frequency).reversed()
    );

    neighbours.forEachEntry(
        (neighId, freq) -> {
          if (sortedNeighbours.size() > MOST_FREQUENT_BIGRAMS_COUNT) {
            sortedNeighbours.pollLast();
          }
          sortedNeighbours.add(new IdFrequencyPair(neighId, freq));
          return true;
        }
    );

    List<TermStatistics.TermFrequency> termFrequencies = new LinkedList<>();
    sortedNeighbours.forEach(
        p -> termFrequencies.add(TermStatistics.TermFrequency
            .newBuilder()
            .setTermFrequency(p.frequency())
            .setTermId(p.termId())
            .build()
        )
    );

    return termFrequencies;
  }

  void build() throws IOException {
    WriteBatch writeBatch = statisticsDb.createWriteBatch();
    wordFrequencyMap.forEachKey(
        k -> {
          writeBatch.put(
              ByteBuffer.allocate(4).putInt(k).array(),
              IndexUnits.TermStatistics
                  .newBuilder()
                  .setTermId(k)
                  .setTermFrequency(wordFrequencyMap.get(k))
                  .setDocuementFrequency(documentFrequencyMap.get(k))
                  .addAllBigramFrequency(mostFrequentBigrams(largeBigramsMap.get(k)))
                  .build().toByteArray()
          );
          return true;
        }
    );

    statisticsDb.write(writeBatch, DEFAULT_WRITE_OPTIONS);
    statisticsDb.close();
  }


  private static class IdFrequencyPair {

    int termId;
    int frequency;

    IdFrequencyPair(int termId, int frequency) {
      this.termId = termId;
      this.frequency = frequency;
    }

    int termId() {
      return termId;
    }

    int frequency() {
      return frequency;
    }
  }
}




