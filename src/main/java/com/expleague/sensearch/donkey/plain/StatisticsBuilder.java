package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.IncrementalBuilder;
import com.expleague.sensearch.donkey.RecoverableBuilder.BuilderState;
import com.expleague.sensearch.donkey.utils.TermsCache.ParsedTerm;
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
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

public class StatisticsBuilder implements AutoCloseable, IncrementalBuilder {

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
  private final ThreadLocal<Boolean> isProcessingPage = ThreadLocal.withInitial(() -> false);
  private final ThreadLocal<TLongList> pageTermsSequence = ThreadLocal
      .withInitial(TLongArrayList::new);
  private final ThreadLocal<TLongList> pageLemmasSequence = ThreadLocal
      .withInitial(TLongArrayList::new);

  private final Path termStatisticsPath;
  private final List<StatisticsBuilderState> priorStates = new ArrayList<>();

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
          TLongIntMap neighFreq = bigramsFreq.get(id);
          localBigrams.get(id).forEachEntry(
              (nId, nFreq) -> {
                neighFreq.adjustOrPutValue(nId, nFreq, nFreq);
                return true;
              }
          );
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
    wordFreq.forEachEntry(
        (id, freq) -> {
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
    for (StatisticsBuilderState state : priorStates) {
      state.termFrequency().forEachEntry((id, freq) -> {
        termFrequency.adjustOrPutValue(id, freq, freq);
        return true;
        }
      );
      state.termDocFrequency().forEachEntry((id, freq) -> {
        termDocFrequency.adjustOrPutValue(id, freq, freq);
        return true;
      });
      state.termBigrams().forEachEntry(
          (id, neigh) -> {
            TLongIntMap neighTrg = termsBigrams.get(id);
            neigh.forEachEntry((idN, freqN) -> {
              neighTrg.adjustOrPutValue(idN, freqN, freqN);
              return true;
            });
            return true;
          }
      );
      state.free();
    }
    writeStatistics(termFrequency, termDocFrequency, termsBigrams, statisticsDb);
    statisticsDb.close();
  }

  @Override
  public synchronized void setStates(BuilderState... increments) {
    resetState();
    priorStates.clear();
    priorStates.addAll(IncrementalBuilder.accumulate(StatisticsBuilderState.class, increments));
  }

  @Override
  public synchronized BuilderState state() {
    StatisticsBuilderState state = new StatisticsBuilderState(this);
    priorStates.add(state);
    resetState();
    return state;
  }

  private synchronized void resetState() {
    termFrequency.clear();
    termDocFrequency.clear();
    termsBigrams.clear();
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

  static class StatisticsBuilderState implements BuilderState {
    private static final String TERM_FREQ_PROP = "termFreq";
    private static final String TERM_DOC_FREQ_PROP = "termDocFreq";
    private static final String TERM_BIGRAMS_PROP = "termBigrams";

    private TLongLongMap termFrequency = null;
    private TLongIntMap termDocFrequency = null;
    private TLongObjectMap<TLongIntMap> termBigrams = null;

    private Path root = null;
    private StateMeta meta = null;

    /**
     * Deep copies state of the owner
     * @param owner StatisticsBuilder ot create state of
     */
    private StatisticsBuilderState(StatisticsBuilder owner) {
      this.termFrequency = new TLongLongHashMap();
      this.termFrequency.putAll(owner.termFrequency);
      this.termDocFrequency = new TLongIntHashMap();
      this.termDocFrequency.putAll(owner.termDocFrequency);
      this.termBigrams = new TLongObjectHashMap<>();
      owner.termsBigrams.forEachEntry(
          (id, map) -> {
            TLongIntMap neighFreq = new TLongIntHashMap();
            neighFreq.putAll(map);
            this.termBigrams.put(id, neighFreq);
            return true;
          }
      );
    }

    private StatisticsBuilderState(Path root, StateMeta meta) {
      this.root = root;
      this.meta = meta;
    }

    public static BuilderState loadFrom(Path from) throws IOException {
      return BuilderState.loadFrom(from, StatisticsBuilderState.class, LOG);
    }

    private TLongLongMap termFrequency() {
      if (termFrequency != null) {
        return termFrequency;
      }

      if (meta == null || root == null) {
        throw new IllegalStateException("Either terms list or meta file must be non null!");
      }

      termFrequency = BuilderState.loadObject(meta, TERM_FREQ_PROP, root, TLongLongMap.class);
      return termFrequency;
    }

    private TLongIntMap termDocFrequency() {
      if (termDocFrequency != null) {
        return termDocFrequency;
      }

      if (meta == null || root == null) {
        throw new IllegalStateException("Either terms list or meta file must be non null!");
      }

      termDocFrequency = BuilderState.loadObject(meta, TERM_DOC_FREQ_PROP, root, TLongIntMap.class);
      return termDocFrequency;
    }

    @SuppressWarnings("unchecked")
    private TLongObjectMap<TLongIntMap> termBigrams() {
      if (termBigrams != null) {
        return termBigrams;
      }

      if (meta == null || root == null) {
        throw new IllegalStateException("Either terms list or meta file must be non null!");
      }

      termBigrams = BuilderState.loadObject(meta, TERM_BIGRAMS_PROP, root, TLongObjectMap.class);
      return termBigrams;
    }

    private void free() {
      if (meta == null || root == null) {
        throw new IllegalStateException("State is not saved and cannot be freed");
      }
      termFrequency = null;
      termDocFrequency = null;
      termBigrams = null;
    }

    @Override
    public void saveTo(Path to) throws IOException {
      if (Files.exists(to)) {
        throw new IOException(String.format("Path [ %s ] already exists!", to.toString()));
      }

      Files.createDirectories(to);
      String termFreqFileName = "termFreq.map";
      String termDocFreqFileName = "termDocFreq.map";
      String termBigramsFileName = "termBigrams.map";

      meta = StateMeta.builder(StatisticsBuilderState.class)
          .addProperty(TERM_FREQ_PROP, termFreqFileName)
          .addProperty(TERM_DOC_FREQ_PROP, termDocFreqFileName)
          .addProperty(TERM_BIGRAMS_PROP, termBigramsFileName)
          .build();
      meta.writeTo(to.resolve(META_FILE));

      try(ObjectOutputStream serializer = new ObjectOutputStream(Files
          .newOutputStream(to.resolve(termFreqFileName)))) {
        serializer.writeObject(termFrequency);
      }

      try(ObjectOutputStream serializer = new ObjectOutputStream(Files
          .newOutputStream(to.resolve(termDocFreqFileName)))) {
        serializer.writeObject(termDocFrequency);
      }

      try(ObjectOutputStream serializer = new ObjectOutputStream(Files
          .newOutputStream(to.resolve(termBigramsFileName)))) {
        serializer.writeObject(termBigrams);
      }

      root = to;
      free();
    }
  }
}
