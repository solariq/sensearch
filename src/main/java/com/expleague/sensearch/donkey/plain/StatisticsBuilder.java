package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.utils.ParsedTerm;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics.TermFrequency;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.primitives.Longs;
import gnu.trove.TCollections;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsBuilder implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(StatisticsBuilder.class);

  private static final long DEFAULT_CACHE_SIZE = 1 << 20;
  private static final int DEFAULT_BLOCK_SIZE = 1 << 20;
  private static final Options STATS_DB_OPTIONS =
      new Options()
          .cacheSize(DEFAULT_CACHE_SIZE)
          .blockSize(DEFAULT_BLOCK_SIZE) // 1 MB
          .createIfMissing(true)
          .errorIfExists(true)
          .compressionType(CompressionType.SNAPPY);
  private static final WriteOptions DEFAULT_WRITE_OPTIONS =
      new WriteOptions().sync(true).snapshot(false);

  private static final int MOST_FREQUENT_BIGRAMS_COUNT = 10;

  private final TLongLongMap termFrequency = TCollections
      .synchronizedMap(new TLongLongHashMap());
  private final TLongIntMap termDocFrequency = TCollections
      .synchronizedMap(new TLongIntHashMap());
  private final TLongObjectMap<TLongIntMap> termsBigrams = TCollections
      .synchronizedMap(new TLongObjectHashMap<>());

  // page-local states
  // TODO: this is a correct usage of thread locals?
  private final ThreadLocal<Boolean> isProcessingPage = ThreadLocal
      .withInitial(() -> false);
  private final ThreadLocal<TLongList> pageTermsSequence = ThreadLocal
      .withInitial(TLongArrayList::new);
  private final ThreadLocal<TLongList> pageLemmasSequence = ThreadLocal
      .withInitial(TLongArrayList::new);

  private final Path termStatisticsPath;

  StatisticsBuilder(Path termStatisticsPath) {
    this.termStatisticsPath = termStatisticsPath;
    this.isProcessingPage.set(false);
  }

  @VisibleForTesting
  static Iterable<TermStatistics.TermFrequency> mostFrequentBigrams(
      TLongIntMap neighbours, int keep) {
    if (neighbours == null || neighbours.isEmpty()) {
      return new LinkedList<>();
    }

    MinMaxPriorityQueue<IdFrequencyPair> neighboursHeap =
        MinMaxPriorityQueue.orderedBy(
            Comparator.comparingInt(IdFrequencyPair::frequency).reversed())
            .maximumSize(keep)
            .expectedSize(keep)
            .create();

    neighbours.forEachEntry(
        (neighId, freq) -> {
          neighboursHeap.add(new IdFrequencyPair(neighId, freq));
          return true;
        });

    final TermFrequency.Builder tfBuilder = TermFrequency.newBuilder();
    final List<TermStatistics.TermFrequency> termFrequencies = new LinkedList<>();
    neighboursHeap.forEach(
        p ->
            termFrequencies.add(
                tfBuilder.setTermFrequency(p.frequency()).setTermId(p.termId()).build()));

    return termFrequencies;
  }

  /**
   * Sync-friendly
   */
  @VisibleForTesting
  static void incrementStatsFromSequence(final TLongList wordSequence, final TLongLongMap wordFreq,
      final TLongIntMap docFreq, final TLongObjectMap<TLongIntMap> bigramsFreq) {
    if (wordSequence.isEmpty()) {
      LOG.warn("Tried to increment stats form empty sequence");
      return;
    }

    TLongLongMap localWordFreq = new TLongLongHashMap();
    TLongObjectMap<TLongIntMap> localBigrams = new TLongObjectHashMap<>();
    localWordFreq.adjustOrPutValue(wordSequence.get(0), 1, 1);
    int seqLen = wordSequence.size();
    for (int i = 1; i < seqLen; i++) {
      long prevWordId = wordSequence.get(i - 1);
      long curWordId = wordSequence.get(i);
      localWordFreq.adjustOrPutValue(curWordId, 1, 1);
      localBigrams.putIfAbsent(prevWordId, new TLongIntHashMap());
      localBigrams.get(prevWordId).adjustOrPutValue(curWordId, 1, 1);
    }

    localWordFreq.forEachEntry(
        (id, freq) -> {
          wordFreq.adjustOrPutValue(id, freq, freq);
          docFreq.adjustOrPutValue(id, 1, 1);
          bigramsFreq.putIfAbsent(id, new TLongIntHashMap());
          TLongIntMap neighFreq = bigramsFreq.get(id);
          if (localBigrams.containsKey(id)) {
            localBigrams.get(id).forEachEntry(
                (nId, nFreq) -> {
                  neighFreq.adjustOrPutValue(nId, nFreq, nFreq);
                  return true;
                }
            );
          }
          return true;
        }
    );
  }

  public void startPage() {
    if (isProcessingPage.get()) {
      throw new IllegalStateException("Duplicate startPage call: page is already being processed");
    }
    isProcessingPage.set(true);
  }

  public void endPage() {
    if (!isProcessingPage.get()) {
      throw new IllegalStateException("Illegal call to endPage: no page is being processed");
    }
    isProcessingPage.set(false);
    incrementStatsFromSequence(pageTermsSequence.get(), termFrequency,
        termDocFrequency, termsBigrams);
    incrementStatsFromSequence(pageLemmasSequence.get(), termFrequency,
        termDocFrequency, termsBigrams);
    pageTermsSequence.get().clear();
    pageLemmasSequence.get().clear();
  }

  // TODO: save lemma statistics
  void enrich(ParsedTerm parsedTerm) {
    pageTermsSequence.get().add(parsedTerm.wordId());
    if (parsedTerm.hasLemma()) {
      pageLemmasSequence.get().add(parsedTerm.lemmaId());
    }
  }

  private static void writeStatistics(TLongLongMap wordFreq, TLongIntMap docFreq,
      TLongObjectMap<TLongIntMap> bigramsMap, DB statisticsDb) {
    WriteBatch writeBatch = statisticsDb.createWriteBatch();
    final TermStatistics.Builder tsBuilder = TermStatistics.newBuilder();
    wordFreq.forEachEntry((id, freq) -> {
      writeBatch.put(
          Longs.toByteArray(id),
          tsBuilder
              .setTermId(id)
              .setTermFrequency(freq)
              .setDocumentFrequency(docFreq.get(id))
              .addAllBigramFrequency(
                  mostFrequentBigrams(bigramsMap.get(id), MOST_FREQUENT_BIGRAMS_COUNT)
              )
              .build()
              .toByteArray());

      tsBuilder.clear();
      return true;
    });
    statisticsDb.write(writeBatch, DEFAULT_WRITE_OPTIONS);
  }

  @Override
  // TODO: check that all pages are ended!
  public void close() throws IOException {
    LOG.info("Storing statistics...");
    DB statisticsDb = JniDBFactory.factory.open(termStatisticsPath.toFile(), STATS_DB_OPTIONS);
    writeStatistics(termFrequency, termDocFrequency, termsBigrams, statisticsDb);
    statisticsDb.close();
  }

  private static class IdFrequencyPair {

    final long termId;
    final int frequency;

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
