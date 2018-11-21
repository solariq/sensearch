package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.index.embedding.Embedding;
import com.expleague.sensearch.index.embedding.Filter;
import com.expleague.sensearch.index.embedding.impl.EmbeddingImpl;
import com.expleague.sensearch.index.embedding.impl.FilterImpl;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import com.google.common.primitives.Longs;
import com.google.protobuf.InvalidProtocolBufferException;
import gnu.trove.list.TLongList;
import gnu.trove.list.linked.TLongLinkedList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;

public class PlainIndex implements Index {

  private static final long DEFAULT_CACHE_SIZE = 128 * (1 << 20); // 128 MB

  private static final Options DEFAULT_DB_OPTIONS = new Options()
      .cacheSize(DEFAULT_CACHE_SIZE)
      .createIfMissing(false)
      .errorIfExists(true)
      .compressionType(CompressionType.SNAPPY);

  private static final ReadOptions DEFAULT_READ_OPTIONS = new ReadOptions()
      .fillCache(true);

  private static final Logger LOG = Logger.getLogger(PlainIndex.class.getName());

  private static final int DOC_NUMBER = 50;
  private static final int SYNONYMS_COUNT = 50;
  private static final Embedding embedding = new EmbeddingImpl(/*smth*/null);
  private static final Filter filter = new FilterImpl(/*smth*/null);

  private final Path indexRoot;

  private final TObjectLongMap<String> wordToIdMap;
  private final TLongObjectMap<String> idToWordMap;

  private final DB termStatisticsBase;
  private final DB plainBase;

  private final double averagePageSize;
  private final int indexSize;
  private final int vocabularySize;

  private TermStatistics lastTermStatistics = null;

  public PlainIndex(Config config) throws IOException {
    indexRoot = config.getTemporaryIndex();

    termStatisticsBase = JniDBFactory.factory.open(
        indexRoot.resolve(PlainIndexBuilder.TERM_STATISTICS_ROOT).toFile(),
        DEFAULT_DB_OPTIONS
    );

    plainBase = JniDBFactory.factory.open(
        indexRoot.resolve(PlainIndexBuilder.PLAIN_ROOT).toFile(),
        DEFAULT_DB_OPTIONS
    );

    IndexUnits.IndexStatistics indexStatistics = IndexUnits.IndexStatistics.parseFrom(
        Files.newInputStream(indexRoot.resolve(PlainIndexBuilder.INDEX_STATISTICS))
    );
    averagePageSize = indexStatistics.getAveragePageSize();
    indexSize = indexStatistics.getPagesCount();
    vocabularySize = indexStatistics.getVocabularySize();

    wordToIdMap = new TObjectLongHashMap<>();
    try (BufferedReader mappingsReader =
        new BufferedReader(
            new InputStreamReader(
                new GZIPInputStream(
                    Files.newInputStream(indexRoot.resolve(PlainIndexBuilder.WORD_ID_MAPPINGS))
                )
            )
        )
    ) {
      String[] tokens = mappingsReader.readLine().split("\t");
      wordToIdMap.put(tokens[0], Long.parseLong(tokens[1]));
    }

    idToWordMap = new TLongObjectHashMap<>();
    wordToIdMap.forEachEntry(
        (word, id) -> {
          idToWordMap.put(id, word);
          return true;
        }
    );
  }

  private static boolean isPageId(long id) {
    return id <= 0;
  }

  private static boolean isWordId(long id) {
    return id > 0;
  }

  private long getWordId(Term term) {
    String normalizedWord = term.getRaw().toString().toLowerCase();
    if (!wordToIdMap.containsKey(normalizedWord)) {
      LOG.fine(String.format("No mapping was found for word %s", normalizedWord));
      throw new NoSuchElementException("No mapping for word %s");
    }

    return wordToIdMap.get(normalizedWord);
  }

  private long[] queryToIds(Query query) {
    TLongList ids = new TLongLinkedList();
    for (Term term : query.getTerms()) {
      try {
        ids.add(getWordId(term));
      } catch (NoSuchElementException e) {
        // ignore
      }
    }

    return ids.toArray();
  }

  /**
   * TODO: What kind of terms is returned?
   */
  @Override
  public Term[] synonyms(Term term) {
    return filter
        .filtrate(
            embedding.getVec(wordToIdMap.get(term.getRaw().toString().toLowerCase())),
            SYNONYMS_COUNT,
            PlainIndex::isWordId
        )
        .mapToObj(this::word)
        .toArray(Term[]::new);
  }

  private IndexedPage page(long id) {
    try {
      return new PlainPage(
          IndexUnits.Page.parseFrom(plainBase.get(Longs.toByteArray(id)))
      );
    } catch (InvalidProtocolBufferException e) {
      LOG.severe("Encountered invalid protobuf in Plain Base!");
      return new PlainPage();
    }
  }

  //todo implement
  private String word(long id) {
    return idToWordMap.get(id);
  }

  public Stream<CharSequence> allTitles() {
    return null;
  }

  @Override
  public Stream<Page> fetchDocuments(Query query) {
    long[] queryIds = queryToIds(query);

    if (queryIds.length == 0) {
      // TODO: process case
    }

    return filter.filtrate(
        embedding.getVec(queryIds),
        DOC_NUMBER,
        PlainIndex::isPageId
    ).mapToObj(this::page);
  }

  @Override
  public int indexSize() {
    return indexSize;
  }

  @Override
  public double averagePageSize() {
    return averagePageSize;
  }

  @Override
  public int documentFrequency(Term term) {
    try {
      long termId = getWordId(term);
      return termStatistics(termId).getDocuementFrequency();
    } catch (DBException | NoSuchElementException e) {
      return 0;
    } catch (InvalidProtocolBufferException e) {
      LOG.severe("Encountered invalid protobuf in Term Statistics Base!");
      return 0;
    }
  }

  @Override
  public long termFrequency(Term term) {
    try {
      long termId = getWordId(term);
      return termStatistics(termId).getTermFrequency();
    } catch (DBException | NoSuchElementException e) {
      return 0;
    } catch (InvalidProtocolBufferException e) {
      LOG.severe("Encountered invalid protobuf in Term Statistics Base!");
      return 0;
    }
  }

  private TermStatistics termStatistics(long termId) throws InvalidProtocolBufferException {
    if (lastTermStatistics == null || lastTermStatistics.getTermId() != termId) {
      lastTermStatistics = TermStatistics.parseFrom(
          termStatisticsBase.get(Longs.toByteArray(termId), DEFAULT_READ_OPTIONS)
      );
    }
    return lastTermStatistics;
  }

  @Override
  public int vocabularySize() {
    return vocabularySize;
  }
}
