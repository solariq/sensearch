package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.donkey.plain.ByteTools;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.index.Embedding;
import com.expleague.sensearch.index.Filter;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta.IdMapping;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics.TermFrequency;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import gnu.trove.list.TLongList;
import gnu.trove.list.linked.TLongLinkedList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.stream.Stream;
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
      .compressionType(CompressionType.SNAPPY);

  private static final ReadOptions DEFAULT_READ_OPTIONS = new ReadOptions()
      .fillCache(true);

  private static final Logger LOG = Logger.getLogger(PlainIndex.class.getName());

  private static final int DOC_NUMBER = 1000;
  private static final int SYNONYMS_COUNT = 50;

  private final Path indexRoot;

  private final TObjectLongMap<String> wordToIdMap;
  private final TLongObjectMap<String> idToWordMap;

  private final DB termStatisticsBase;
  private final DB plainBase;

  private final double averagePageSize;
  private final int indexSize;
  private final int vocabularySize;

  private final BloomFilter<byte[]> titlesBloomFilter;

  private final Embedding embedding;
  private final Filter filter;

  private TermStatistics lastTermStatistics = null;

  public PlainIndex(Config config) throws IOException {
    indexRoot = config.getTemporaryIndex();

    embedding = new EmbeddingImpl(indexRoot.resolve(PlainIndexBuilder.EMBEDDING_ROOT));
    filter = new FilterImpl(embedding);

    termStatisticsBase = JniDBFactory.factory.open(
        indexRoot.resolve(PlainIndexBuilder.TERM_STATISTICS_ROOT).toFile(),
        DEFAULT_DB_OPTIONS
    );

    plainBase = JniDBFactory.factory.open(
        indexRoot.resolve(PlainIndexBuilder.PLAIN_ROOT).toFile(),
        DEFAULT_DB_OPTIONS
    );

    IndexUnits.IndexMeta indexMeta = IndexUnits.IndexMeta.parseFrom(
        Files.newInputStream(indexRoot.resolve(PlainIndexBuilder.INDEX_META_FILE))
    );
    averagePageSize = indexMeta.getAveragePageSize();
    indexSize = indexMeta.getPagesCount();
    vocabularySize = indexMeta.getVocabularySize();

    ByteString byteStringFilter = indexMeta.getTitlesBloomFilter();
    titlesBloomFilter = BloomFilter.readFrom(
        new ByteArrayInputStream(byteStringFilter.toByteArray(), 0, byteStringFilter.size()),
        Funnels.byteArrayFunnel()
    );

    wordToIdMap = new TObjectLongHashMap<>();
    for (IdMapping idMapping : indexMeta.getIdMappingsList()) {
      wordToIdMap.put(idMapping.getWord(), idMapping.getId());
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

  @Override
  public List<String> mostFrequentNeighbours(String rawWord) {
    rawWord = rawWord.toLowerCase().trim();
    long wordId = 0;
    try {
      wordId = toId(rawWord);
      List<String> neighbours = new ArrayList<>();
      for (TermFrequency tf : termStatistics(wordId)
          .getBigramFrequencyList()) {
        neighbours.add(idToWord(tf.getTermId()));
      }
      return neighbours;
    } catch (NoSuchElementException e) {
      return Collections.emptyList();
    } catch (InvalidProtocolBufferException e) {
      LOG.warning(String.format(
          "Encountered invalid protobuf in statistics base for word with id %d", wordId
          )
      );
      return Collections.emptyList();
    }
  }

  @Override
  public boolean hasTitle(CharSequence title) {
    return titlesBloomFilter.mightContain(
        ByteTools.toBytes(
            toIds(Tokenizer.tokenize(title))
        )
    );
  }

  private long toId(String word) {
    word = word.toLowerCase();
    if (!wordToIdMap.containsKey(word)) {
      LOG.fine(String.format("No mapping was found for word %s", word));
      throw new NoSuchElementException("No mapping for word %s");
    }

    return wordToIdMap.get(word);
  }

  private long toId(Term term) {
    return toId(term.getRaw().toString());
  }

  private long[] toIds(String... words) {
    TLongList ids = new TLongLinkedList();
    for (String word : words) {
      try {
        ids.add(toId(word));
      } catch (NoSuchElementException e) {
        // ignore
      }
    }

    return ids.toArray();
  }

  private long[] toIds(Query query) {
    List<String> rawWords = new LinkedList<>();
    query.getTerms().forEach(t -> rawWords.add(t.getRaw().toString().toLowerCase()));
    return toIds(rawWords.toArray(new String[rawWords.size()]));
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
        .mapToObj(this::idToWord)
        .toArray(Term[]::new);
  }

  private IndexedPage idToPage(long id) {
    try {
      return new PlainPage(
          IndexUnits.Page.parseFrom(plainBase.get(Longs.toByteArray(id)))
      );
    } catch (InvalidProtocolBufferException e) {
      LOG.severe("Encountered invalid protobuf in Plain Base!");
      return new PlainPage();
    }
  }

  private String idToWord(long id) {
    return idToWordMap.get(id);
  }

  @Override
  public Stream<Page> fetchDocuments(Query query) {
    long[] queryIds = toIds(query);

    if (queryIds.length == 0) {
      return Stream.empty();
    }

    return filter.filtrate(
        embedding.getVec(queryIds),
        DOC_NUMBER,
        PlainIndex::isPageId
    ).mapToObj(this::idToPage);
  }

  @Override
  public int size() {
    return indexSize;
  }

  @Override
  public double averagePageSize() {
    return averagePageSize;
  }

  @Override
  public int documentFrequency(Term term) {
    try {
      long termId = toId(term);
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
      long termId = toId(term);
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