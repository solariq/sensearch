package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.MyStemImpl;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.index.Embedding;
import com.expleague.sensearch.index.Filter;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta.UriPageMapping;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics.TermFrequency;
import com.expleague.sensearch.query.Query;
import com.google.common.primitives.Longs;
import com.google.protobuf.InvalidProtocolBufferException;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.log4j.Logger;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;
import org.jetbrains.annotations.Nullable;

public class PlainIndex implements Index {

  public static final int VERSION = 5;

  private static final long DEFAULT_CACHE_SIZE = 128 * (1 << 20); // 128 MB

  private static final Options DEFAULT_DB_OPTIONS =
      new Options()
          .cacheSize(DEFAULT_CACHE_SIZE)
          .createIfMissing(false)
          .compressionType(CompressionType.SNAPPY);

  private static final ReadOptions DEFAULT_READ_OPTIONS = new ReadOptions().fillCache(true);

  private static final Logger LOG = Logger.getLogger(PlainIndex.class.getName());

  private static final int FILTERED_DOC_NUMBER = 50;
  private static final int SYNONYMS_COUNT = 50;

  private final Path indexRoot;

  private final Map<CharSeq, Term> wordToTerms = new HashMap<>();
  private final TLongObjectMap<Term> idToTerm = new TLongObjectHashMap<>();
  private final TObjectLongMap<URI> uriToPageIdMap = new TObjectLongHashMap<>();

  private final DB termStatisticsBase;
  private final DB pageBase;
  private final DB termBase;

  private final double averagePageSize;
  private final int indexSize;
  private final int vocabularySize;

  private final Embedding embedding;
  private final Filter filter;
  private final MyStem stemmer;
  private final Tokenizer tokenizer;

  private TermStatistics lastTermStatistics;

  public PlainIndex(Config config) throws IOException {
    LOG.info("Loading PlainIndex...");
    long startTime = System.nanoTime();

    indexRoot = config.getTemporaryIndex();

    embedding = new EmbeddingImpl(indexRoot.resolve(PlainIndexBuilder.EMBEDDING_ROOT));
    filter = new FilterImpl(embedding);

    termStatisticsBase =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.TERM_STATISTICS_ROOT).toFile(), DEFAULT_DB_OPTIONS);

    pageBase =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.PAGE_ROOT).toFile(), DEFAULT_DB_OPTIONS);

    termBase =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.TERM_ROOT).toFile(), DEFAULT_DB_OPTIONS);

    tokenizer = new TokenizerImpl();

    stemmer = new MyStemImpl(config.getMyStem());

    IndexUnits.IndexMeta indexMeta =
        IndexUnits.IndexMeta.parseFrom(
            Files.newInputStream(indexRoot.resolve(PlainIndexBuilder.INDEX_META_FILE)));

    if (indexMeta.getVersion() != VERSION) {
      throw new IllegalArgumentException(
          String.format(
              "Built index has version %d while code version is %d",
              indexMeta.getVersion(), VERSION));
    }
    averagePageSize = indexMeta.getAveragePageSize();
    indexSize = indexMeta.getPagesCount();
    vocabularySize = indexMeta.getVocabularySize();

    final TLongLongMap wordToLemma = new TLongLongHashMap();
    //    indexMeta.getLemmaIdMappingsList().forEach(m -> wordToLemma.put(m.getWordId(),
    // m.getLemmaId()));

    TLongObjectMap<String> idToWord = new TLongObjectHashMap<>();
    //    indexMeta.getIdMappingsList().forEach(m -> idToWord.put(m.getId(), m.getWord()));

    DBIterator termIterator = termBase.iterator();
    termIterator.seekToFirst();
    termIterator.forEachRemaining(
        item -> {
          try {
            IndexUnits.Term protoTerm = IndexUnits.Term.parseFrom(item.getValue());
            final CharSeq word = CharSeq.intern(protoTerm.getText());

            if (wordToTerms.containsKey(word)) {
              return;
            }

            PartOfSpeech pos =
                protoTerm.getPartOfSpeech() == IndexUnits.Term.PartOfSpeech.UNKNOWN
                    ? null
                    : PartOfSpeech.valueOf(protoTerm.getPartOfSpeech().name());

            final IndexTerm lemmaTerm;

            final long lemmaId = wordToLemma.get(protoTerm.getId());
            if (lemmaId == -1) {
              lemmaTerm = null;
            } else {
              if (idToTerm.containsKey(lemmaId)) {
                lemmaTerm = (IndexTerm) idToTerm.get(lemmaId);
              } else {
                CharSeq lemmaText = CharSeq.intern(idToWord.get(lemmaId));

                lemmaTerm = new IndexTerm(this, lemmaText, lemmaId, null, pos);
                idToTerm.put(lemmaId, lemmaTerm);
                wordToTerms.put(lemmaText, lemmaTerm);
              }
            }

            IndexTerm term = new IndexTerm(this, word, protoTerm.getId(), lemmaTerm, pos);
            idToTerm.put(protoTerm.getId(), term);
            wordToTerms.put(word, term);

          } catch (InvalidProtocolBufferException e) {
            LOG.fatal("Invalid protobuf for term with id " + Longs.fromByteArray(item.getKey()));
            throw new RuntimeException(e);
          }
        });

    for (UriPageMapping mapping : indexMeta.getUriPageMappingsList()) {
      uriToPageIdMap.put(URI.create(mapping.getUri()), mapping.getPageId());
    }

    LOG.info(
        String.format("PlainIndex loaded in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }

  private static boolean isPageId(long id) {
    return id <= 0;
  }

  private static boolean isWordId(long id) {
    return id > 0;
  }

  @Override
  public Stream<Term> mostFrequentNeighbours(Term term) {
    try {
      return termStatistics(((IndexTerm) term).id())
          .getBigramFrequencyList()
          .stream()
          .mapToLong(TermFrequency::getTermId)
          .mapToObj(idToTerm::get);
    } catch (InvalidProtocolBufferException e) {
      LOG.warn(
          String.format(
              "Encountered invalid protobuf in statistics base for word with id [%d] and text [%s]",
              ((IndexTerm) term).id(), term.text()));
      return Stream.empty();
    }
  }

  /**
   * TODO: What kind of terms is returned?
   */
  Stream<Term> synonyms(Term term) {
    System.out.println("Synonyms for " + term.text());
    filter
        .filtrate(embedding.vec(((IndexTerm) term).id()), SYNONYMS_COUNT, PlainIndex::isWordId)
        .mapToObj(idToTerm::get).forEach(ttt -> System.out.print(ttt.text() + " "));
    System.out.println();
    return filter
        .filtrate(embedding.vec(((IndexTerm) term).id()), SYNONYMS_COUNT, PlainIndex::isWordId)
        .mapToObj(idToTerm::get);
  }

  @Nullable
  private IndexedPage idToPage(long id) {
    try {
      return new PlainPage(IndexUnits.Page.parseFrom(pageBase.get(Longs.toByteArray(id))));
    } catch (InvalidProtocolBufferException e) {
      LOG.fatal("Encountered invalid protobuf in Plain Base!");
      return null;
    }
  }

  @Nullable
  @Override
  public Page page(URI uri) {
    // TODO: maybe add some sophisticated logic here and in builder like URI normalization
    long pageId = uriToPageIdMap.get(uri);
    return pageId == uriToPageIdMap.getNoEntryValue() ? null : idToPage(pageId);
  }

  @Override
  public Stream<Page> fetchDocuments(Query query) {
    final Vec queryVec = new ArrayVec(embedding.dim());
    query
        .terms()
        .stream()
        .mapToLong(t -> ((IndexTerm) t).id())
        .mapToObj(embedding::vec)
        .forEach(v -> VecTools.append(queryVec, v));
    return filter
        .filtrate(queryVec, FILTERED_DOC_NUMBER, PlainIndex::isPageId)
        .mapToObj(this::idToPage);
  }

  @Override
  public Term term(CharSequence seq) {
    final CharSequence normalized = CharSeqTools.toLowerCase(CharSeqTools.trim(seq));
    return wordToTerms.get(CharSeq.intern(normalized));
  }

  @Override
  public Stream<Term> parse(CharSequence sequence) {
    return tokenizer.parseTextToWords(sequence).map(this::term).filter(Objects::nonNull);
  }

  @Override
  public int size() {
    return indexSize;
  }

  @Override
  public double averagePageSize() {
    return averagePageSize;
  }

  int documentFrequency(Term term) {
    try {
      return termStatistics(((IndexTerm) term).id()).getDocuementFrequency();
    } catch (DBException | NoSuchElementException e) {
      return 0;
    } catch (InvalidProtocolBufferException e) {
      LOG.fatal("Encountered invalid protobuf in Term Statistics Base!");
      return 0;
    }
  }

  int termFrequency(Term term) {
    try {
      return (int) termStatistics(((IndexTerm) term).id()).getTermFrequency();
    } catch (DBException | NoSuchElementException e) {
      return 0;
    } catch (InvalidProtocolBufferException e) {
      LOG.fatal("Encountered invalid protobuf in Term Statistics Base!");
      return 0;
    }
  }

  private TermStatistics termStatistics(long termId) throws InvalidProtocolBufferException {
    if (lastTermStatistics == null || lastTermStatistics.getTermId() != termId) {
      lastTermStatistics =
          TermStatistics.parseFrom(
              termStatisticsBase.get(Longs.toByteArray(termId), DEFAULT_READ_OPTIONS));
    }
    return lastTermStatistics;
  }

  @Override
  public int vocabularySize() {
    return vocabularySize;
  }
}
