package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.seq.CharSeqTools;
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
import com.expleague.sensearch.metrics.FilterMetric;
import com.expleague.sensearch.metrics.LSHSynonymsMetric;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta.UriPageMapping;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics.TermFrequency;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.web.suggest.SuggestInformationLoader;
import com.google.common.collect.Streams;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.log4j.Logger;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;

@Singleton
public class PlainIndex implements Index {

  //TODO: !index version!
  public static final int VERSION = 10;

  private static final long DEFAULT_CACHE_SIZE = 128 * (1 << 20); // 128 MB

  private static final Options DEFAULT_DB_OPTIONS =
      new Options()
          .cacheSize(DEFAULT_CACHE_SIZE)
          .createIfMissing(false)
          .compressionType(CompressionType.SNAPPY);

  private static final ReadOptions DEFAULT_READ_OPTIONS = new ReadOptions().fillCache(true);

  private static final Logger LOG = Logger.getLogger(PlainIndex.class.getName());

  private static final int FILTERED_DOC_NUMBER = 500;
  private static final int SYNONYMS_COUNT = 50;

  private final Path indexRoot;

  private final Map<CharSeq, Term> wordToTerms = new HashMap<>();
  private final TLongObjectMap<Term> idToTerm = new TLongObjectHashMap<>();
  private final TObjectLongMap<URI> uriToPageIdMap = new TObjectLongHashMap<>();

  private final DB termStatisticsBase;
  private final DB pageBase;
  private final DB termBase;

  private final DB suggest_unigram_DB;
  private final DB suggest_multigram_DB;
  private final DB suggest_inverted_index_DB;

  private final double averagePageSize;
  // TODO save at database
  private double averageTitleSize = 0;
  private int titleCnt = 0;
  private double averageLinkSize = 0;
  private int linkCnt = 0;

  private final int indexSize;
  private final int vocabularySize;

  private final Embedding embedding;
  private final Filter filter;
  private final FilterMetric filterMetric = new FilterMetric();
  private final LSHSynonymsMetric lshSynonymsMetric;
  private final Tokenizer tokenizer;

  private TermStatistics lastTermStatistics;

  private SuggestInformationLoader suggestLoader;

  @Inject
  public PlainIndex(Config config) throws IOException {

    LOG.info("Loading PlainIndex...");
    long startTime = System.nanoTime();

    indexRoot = config.getTemporaryIndex();

    Path embeddingPath = indexRoot.resolve(PlainIndexBuilder.EMBEDDING_ROOT);
    embedding = new EmbeddingImpl(embeddingPath);
    lshSynonymsMetric =
        new LSHSynonymsMetric(embeddingPath.resolve(PlainIndexBuilder.LSH_METRIC_ROOT));
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

    suggest_unigram_DB =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.SUGGEST_UNIGRAM_ROOT).toFile(), DEFAULT_DB_OPTIONS);

    suggest_multigram_DB =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.SUGGEST_MULTIGRAMS_ROOT).toFile(),
            DEFAULT_DB_OPTIONS);

    suggest_inverted_index_DB =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.SUGGEST_INVERTED_INDEX_ROOT).toFile(),
            DEFAULT_DB_OPTIONS);

    tokenizer = new TokenizerImpl();

    IndexUnits.IndexMeta indexMeta =
        IndexUnits.IndexMeta.parseFrom(
            Files.newInputStream(indexRoot.resolve(PlainIndexBuilder.INDEX_META_FILE)));

    if (indexMeta.getVersion() != VERSION) {
      String errorMessage =
          String.format(
              "Built index has version %d while code version is %d",
              indexMeta.getVersion(), VERSION);
      LOG.fatal(errorMessage);
      throw new IllegalArgumentException(errorMessage);
    }
    averagePageSize = indexMeta.getAveragePageSize();
    indexSize = indexMeta.getPagesCount();
    vocabularySize = indexMeta.getVocabularySize();

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

            final long lemmaId = protoTerm.getLemmaId();
            if (lemmaId == -1) {
              lemmaTerm = null;
            } else {
              if (idToTerm.containsKey(lemmaId)) {
                lemmaTerm = (IndexTerm) idToTerm.get(lemmaId);
              } else {
                CharSeq lemmaText =
                    CharSeq.intern(
                        IndexUnits.Term.parseFrom(termBase.get(Longs.toByteArray(lemmaId)))
                            .getText());

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

    DBIterator pageIterator = pageBase.iterator();
    pageIterator.seekToFirst();
    pageIterator.forEachRemaining(
        page -> {
          try {
            IndexUnits.Page protoPage = IndexUnits.Page.parseFrom(page.getValue());
            averageTitleSize += parse(protoPage.getTitle()).count();
            titleCnt++;
            for (int i = 0; i < protoPage.getIncomingLinksCount(); i++) {
              linkCnt++;
              averageLinkSize +=
                  parse(protoPageLoad(protoPage.getIncomingLinks(i).getTargetPageId()).getTitle())
                      .count();
            }
          } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
          }
        });

    averageTitleSize = averageTitleSize / titleCnt;
    averageLinkSize = averageLinkSize / linkCnt;

    for (UriPageMapping mapping : indexMeta.getUriPageMappingsList()) {
      uriToPageIdMap.put(URI.create(mapping.getUri()), mapping.getPageId());
    }

    LOG.info(
        String.format("PlainIndex loaded in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }

  static boolean isPageId(long id) {
    return id <= 0;
  }

  static boolean isWordId(long id) {
    return id > 0;
  }

  IndexUnits.Page protoPageLoad(long id) throws InvalidProtocolBufferException {
    byte[] pageBytes = pageBase.get(Longs.toByteArray(id));
    if (pageBytes == null) {
      throw new NoSuchElementException(
          String.format("No page with id [ %d ] was found in the index!", id));
    }

    return IndexUnits.Page.parseFrom(pageBytes);
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
              "Encountered invalid protobuf in statistics base for word with id [%d] and content [%s]",
              ((IndexTerm) term).id(), term.text()));
      return Stream.empty();
    }
  }

  @Override
  public Page page(URI uri) {
    if (!uriToPageIdMap.containsKey(uri)) {
      return PlainPage.EMPTY_PAGE;
    }
    return PlainPage.create(uriToPageIdMap.get(uri), this);
  }

  @Override
  public Vec vecByTerms(List<Term> terms) {
    final ArrayVec answerVec = new ArrayVec(embedding.dim());
    long cnt = terms
        .stream()
        .mapToLong(t -> ((IndexTerm) t).id())
        .mapToObj(embedding::vec)
        .filter(Objects::nonNull)
        .peek(v -> VecTools.append(answerVec, v)).count();
    if (cnt > 0) {
      answerVec.scale(1. / ((double) cnt));
    }
    return answerVec;
  }

  @Override
  public Stream<Page> fetchDocuments(Query query) {
    final Vec mainVec = vecByTerms(query.terms());
    return filter
        .filtrate(mainVec, 0.5, PlainIndex::isPageId)
        .limit(FILTERED_DOC_NUMBER)
        .mapToObj(id -> PlainPage.create(id, this));
  }

  @Override
  public Stream<Page> allDocuments() {
    DBIterator iterator = pageBase.iterator();
    iterator.seekToFirst();
    return Streams.stream(iterator)
        .map(entry -> (Page) PlainPage.create(Longs.fromByteArray(entry.getKey()), this))
        .filter(page -> !page.hasParent());
  }

  @Override
  public Term term(CharSequence seq) {
    final CharSequence normalized = CharSeqTools.toLowerCase(CharSeqTools.trim(seq));
    return wordToTerms.get(CharSeq.intern(normalized));
  }

  @Override
  public Stream<CharSequence> sentences(CharSequence sequence) {
    return tokenizer.toSentences(sequence);
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

  @Override
  public double averageTitleSize() {
    return averageTitleSize;
  }

  @Override
  public double averageLinkSize() {
    return averageLinkSize;
  }

  @Override
  public int vocabularySize() {
    return vocabularySize;
  }

  Stream<Term> synonyms(Term term) {
    //    System.out.println("Synonyms for " + term.text());
    Vec termVec = embedding.vec(((IndexTerm) term).id());
    if (termVec == null) {
      return Stream.empty();
    }

    Set<Long> LSHIds =
        filter
            .filtrate(termVec, SYNONYMS_COUNT, PlainIndex::isWordId)
            .boxed()
            .collect(Collectors.toSet());

    double result = lshSynonymsMetric.calc(((IndexTerm) term).id(), LSHIds);

    LOG.info("LSHSynonymsMetric: " + result);

    return filter.filtrate(termVec, SYNONYMS_COUNT, PlainIndex::isWordId).mapToObj(idToTerm::get);
  }

  int documentFrequency(Term term) {
    try {
      return termStatistics(((IndexTerm) term).id()).getDocumentFrequency();
    } catch (DBException | NoSuchElementException | NullPointerException e) {
      return 0;
    } catch (InvalidProtocolBufferException e) {
      LOG.fatal("Encountered invalid protobuf in Term Statistics Base!");
      return 0;
    }
  }

  int documentLemmaFrequency(IndexTerm term) {
    try {
      return termStatistics((term).id()).getDocumentLemmaFrequency();
    } catch (DBException | NoSuchElementException | NullPointerException e) {
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

  TermStatistics termStatistics(long termId) throws InvalidProtocolBufferException {
    if (lastTermStatistics == null || lastTermStatistics.getTermId() != termId) {
      lastTermStatistics =
          TermStatistics.parseFrom(
              termStatisticsBase.get(Longs.toByteArray(termId), DEFAULT_READ_OPTIONS));
    }
    return lastTermStatistics;
  }

  // однопоточно
  @Override
  public SuggestInformationLoader getSuggestInformation() {
    if (suggestLoader == null) {
      suggestLoader =
          new SuggestInformationLoader(
              suggest_unigram_DB, suggest_multigram_DB, suggest_inverted_index_DB, idToTerm);
    }
    return suggestLoader;
  }
}
