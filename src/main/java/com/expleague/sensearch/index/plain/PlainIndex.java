package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Annotations.IndexRoot;
import com.expleague.sensearch.core.IdUtils;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.core.Term.TermAndDistance;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.features.FeaturesForRequiredDocument;
import com.expleague.sensearch.features.FeaturesImpl;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.features.sets.filter.FilterDistFeatureSet;
import com.expleague.sensearch.filter.Filter;
import com.expleague.sensearch.index.Embedding;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.metrics.LSHSynonymsMetric;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics.TermFrequency;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.term.TermBase;
import com.expleague.sensearch.web.suggest.SuggestInformationLoader;
import com.google.common.collect.Streams;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import gnu.trove.list.TIntList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.log4j.Logger;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;

public class PlainIndex implements Index {

  // TODO: !index version!
  public static final int VERSION = 15;

  private static final long DEFAULT_CACHE_SIZE = 128 * (1 << 20); // 128 MB

  private static final Options DEFAULT_DB_OPTIONS =
      new Options()
          .cacheSize(DEFAULT_CACHE_SIZE)
          .createIfMissing(false)
          .compressionType(CompressionType.SNAPPY);

  private static final ReadOptions DEFAULT_READ_OPTIONS = new ReadOptions().fillCache(true);

  private static final Logger LOG = Logger.getLogger(PlainIndex.class.getName());

  private static final int SYNONYMS_COUNT = 50;

  private final DB termStatisticsBase;
  private final DB pageBase;

  private final TermBase termBase;

  private DB suggestUnigramDb;
  private DB suggestMultigramDb;

  private final DB uriMappingDb;

  private final double averagePageSize;
  private double averageSectionTitleSize;
  private int sectionTitlesCount;
  private double averageLinkTargetTitleWordCount;
  private int linksCount;

  private final int indexSize;
  private final int vocabularySize;

  private final Embedding embedding;
  private final Filter filter;
  private final LSHSynonymsMetric lshSynonymsMetric;
  private final Tokenizer tokenizer;

  private TermStatistics lastTermStatistics;

  private SuggestInformationLoader suggestLoader;

  private final Map<Term, TIntList> rareTermsInvIdx;

  private final Path indexRoot;
  @Override
  public void close() throws Exception {
    embedding.close();
    pageBase.close();
    termBase.close();
    termStatisticsBase.close();
    uriMappingDb.close();
    if(suggestUnigramDb != null)
      suggestUnigramDb.close();
    if (suggestMultigramDb != null)
      suggestMultigramDb.close();
  }

  @Inject
  public PlainIndex(@IndexRoot Path indexRoot, Embedding embedding, Filter filter)
      throws IOException {

    this.indexRoot = indexRoot;
    
    this.embedding = embedding;
    this.filter = filter;

    LOG.info("Loading PlainIndex...");
    long startTime = System.nanoTime();

    Path embeddingPath = indexRoot.resolve(PlainIndexBuilder.EMBEDDING_ROOT);
    lshSynonymsMetric =
        new LSHSynonymsMetric(embeddingPath.resolve(PlainIndexBuilder.LSH_METRIC_ROOT));

    termStatisticsBase =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.TERM_STATISTICS_ROOT).toFile(), DEFAULT_DB_OPTIONS);

    pageBase =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.PAGE_ROOT).toFile(), DEFAULT_DB_OPTIONS);

    //TODO: add lemmer
    termBase = new TermBase(Paths.get(PlainIndexBuilder.TERM_ROOT), this);

    uriMappingDb =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.URI_MAPPING_ROOT).toFile(), DEFAULT_DB_OPTIONS);

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
    linksCount = indexMeta.getLinksCount();
    averageLinkTargetTitleWordCount = indexMeta.getAverageLinkTargetTitleWordCount();
    sectionTitlesCount = indexMeta.getSectionTitlesCount();
    averageSectionTitleSize = indexMeta.getAverageSectionTitleSize();


    rareTermsInvIdx = new HashMap<>();

    // TODO: uncomment it
//    if (Files.exists(indexRoot.resolve(PlainIndexBuilder.RARE_INV_IDX_FILE))) {
//      ObjectMapper mapper = new ObjectMapper();
//      TLongObjectMap<TLongList> invIdxIds =
//          mapper.readValue(
//              indexRoot.resolve(PlainIndexBuilder.RARE_INV_IDX_FILE).toFile(),
//              new TypeReference<TLongObjectMap<TLongList>>() {});
//
//      invIdxIds.forEachEntry(
//          (t, docs) -> {
//            Term term = idToTerm.get(t);
//            TLongList l = new TLongArrayList();
//
//            rareTermsInvIdx.put(term, l);
//
//            docs.forEach(l::add);
//            return true;
//          });
//    }

    LOG.info(
        String.format("PlainIndex loaded in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }

  static boolean isSectionId(long id) {
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
          .mapToInt(TermFrequency::getTermId)
          .mapToObj(termBase::term);
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
    byte[] result = uriMappingDb.get(uri.toASCIIString().getBytes());
    if (result == null) {
      return PlainPage.EMPTY_PAGE;
    }

    try {
      return PlainPage.create(IndexUnits.UriPageMapping.parseFrom(result).getPageId(), this);
    } catch (InvalidProtocolBufferException e) {
      LOG.warn(e);
      return PlainPage.EMPTY_PAGE;
    }
  }

  @Override
  public Vec vecByTerms(List<Term> terms) {
    final ArrayVec answerVec = new ArrayVec(embedding.dim());
    long cnt =
        terms
            .stream()
            .mapToLong(t -> ((IndexTerm) t).id())
            .mapToObj(embedding::vec)
            .filter(Objects::nonNull)
            .peek(v -> VecTools.append(answerVec, v))
            .count();
    if (cnt > 0) {
      answerVec.scale(1. / ((double) cnt));
    }
    return answerVec;
  }

  public double tfidf(Term t1) {
    IndexTerm t = (IndexTerm) t1;
    return t.freq() * Math.log(1.0 * size() / t.documentFreq());
  }
  
  public Vec weightedVecByTerms(List<Term> terms) {
    final ArrayVec answerVec = new ArrayVec(embedding.dim());
    long cnt =
        terms
        .stream()
        .map(t -> (IndexTerm) t)
        .map(t -> {
          Vec v1 = embedding.vec(t.id());
          if (v1 == null) {
            return null;
          }
          v1 = VecTools.copy(v1);
          return VecTools.scale(v1, 1.0 / t.documentFreq());
        })
        .filter(Objects::nonNull)
        .peek(v -> VecTools.append(answerVec, v))
        .count();
    if (cnt > 0) {
      answerVec.scale(1. / ((double) cnt));
    }
    return answerVec;
  }
  
  @Override
  public Stream<Page> allDocuments() {
    DBIterator iterator = pageBase.iterator();
    iterator.seekToFirst();
    return Streams.stream(iterator)
        .map(entry -> (Page) PlainPage.create(Longs.fromByteArray(entry.getKey()), this))
        .filter(page -> !page.isRoot());
  }

  @Override
  public Term term(CharSequence seq) {
    final CharSequence normalized = CharSeqTools.toLowerCase(CharSeqTools.trim(seq));
    return termBase.term(normalized);
  }

  Term term(int id) {
    return termBase.term(id);
  }

  @Override
  public Stream<List<Term>> sentences(IntStream text) {
    List<Integer> intText = text.boxed().collect(Collectors.toList());
    List<Term> termText = intText.stream().map(this::term).collect(Collectors.toList());
    return tokenizer.toSentences(intText, termText);
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
  public double averageSectionTitleSize() {
    return averageSectionTitleSize;
  }

  @Override
  public double averageLinkTargetTitleWordCount() {
    return averageLinkTargetTitleWordCount;
  }

  @Override
  public int vocabularySize() {
    return vocabularySize;
  }

  Stream<Term> synonyms(Term term) {
    return synonymsWithDistance(term).map(TermAndDistance::term);
  }

  Stream<Term> synonyms(Term term, double synonymThreshold) {
    return synonymsWithDistance(term, synonymThreshold).map(TermAndDistance::term);
  }

  Stream<TermAndDistance> synonymsWithDistance(Term term, double synonymThreshold) {
    Vec termVec = embedding.vec(((IndexTerm) term).id());
    if (termVec == null) {
      return Stream.empty();
    }

    // TODO: FATAL: this nonNull filter must be redundant (but it's necessary for full ruWiki)
    return filter
        .filtrate(termVec, PlainIndex::isWordId, synonymThreshold)
        .filter(Objects::nonNull)
        .map(c -> new IndexTerm.IndexTermAndDistance(termBase.term((int) c.getId()), c.getDist()));
  }

  Stream<TermAndDistance> synonymsWithDistance(Term term) {
    Vec termVec = embedding.vec(((IndexTerm) term).id());
    if (termVec == null) {
      return Stream.empty();
    }

    // TODO: FATAL: this nonNull filter must be redundant (but it's necessary for full ruWiki)
    return filter
        .filtrate(termVec, PlainIndex::isWordId, SYNONYMS_COUNT)
        .filter(Objects::nonNull)
        .map(c -> new IndexTerm.IndexTermAndDistance(termBase.term((int) c.getId()), c.getDist()));
  }

  public Stream<Term> nearestTerms(Vec vec) {
    return embedding
        .nearest(vec, PlainIndex::isWordId)
        .filter(Objects::nonNull)
        .map(c -> termBase.term((int) c.getId()));
  }

  @Override
  public Features filterFeatures(Query query, URI pageURI) {
    double minTitle = 1;
    double minBody = 1;
    double minLink = 1;

    Vec queryVec = query.vec();
    IndexedPage page = (IndexedPage) page(pageURI);
    long tmpID;
    // Title
    tmpID = IdUtils.toStartSecTitleId(page.id());
    Vec pageVec = embedding.vec(tmpID);
    while (pageVec != null) {
      minTitle = Math.min(minTitle, (1.0 - VecTools.cosine(queryVec, pageVec)) / 2.0);
      tmpID++;
      pageVec = embedding.vec(tmpID);
    }
    // Body
    tmpID = IdUtils.toStartSecTextId(page.id());
    pageVec = embedding.vec(tmpID);
    while (pageVec != null) {
      minBody = Math.min(minBody, (1.0 - VecTools.cosine(queryVec, pageVec)) / 2.0);
      tmpID++;
      pageVec = embedding.vec(tmpID);
    }
    // Link
    tmpID = IdUtils.toStartLinkId(page.id());
    pageVec = embedding.vec(tmpID);
    while (pageVec != null) {
      minLink = Math.min(minLink, (1.0 - VecTools.cosine(queryVec, pageVec)) / 2.0);
      tmpID++;
      pageVec = embedding.vec(tmpID);
    }

    return new FeaturesImpl(new FilterDistFeatureSet(), new ArrayVec(minTitle, minBody, minLink));
  }

  @Override
  public Map<Page, Features> fetchDocuments(Query query, int num) {
    FilterDistFeatureSet filterDistFs = new FilterDistFeatureSet();

    List<Term> queryTerms = query.terms();
    final Vec qVec = query.vec();
    TLongObjectMap<List<Candidate>> pageIdToCandidatesMap = new TLongObjectHashMap<>();
    filter
        .filtrate(qVec, PlainIndex::isSectionId, 0.5)
        .limit(num)
        .forEach(
            candidate -> {
              long pageId = candidate.getPageId();
              if (!pageIdToCandidatesMap.containsKey(pageId)) {
                pageIdToCandidatesMap.put(pageId, new ArrayList<>());
              }
              pageIdToCandidatesMap.get(pageId).add(candidate);
            });
    Map<Page, Features> allFilterFeatures = new HashMap<>();
    pageIdToCandidatesMap.forEachEntry(
        (pageId, candidates) -> {
          Page page = PlainPage.create(pageId, this);
          filterDistFs.accept(new QURLItem(page, query));
          candidates.forEach(
              candidate -> {
                long id = candidate.getId();
                if (IdUtils.isSecTitleId(id)) {
                  filterDistFs.withTitle(candidate.getDist());
                } else if (IdUtils.isSecTextId(id)) {
                  filterDistFs.withBody(candidate.getDist());
                } else if (IdUtils.isLinkId(id)) {
                  filterDistFs.withLink(candidate.getDist());
                }
              });
          Vec vec = filterDistFs.advance();
          allFilterFeatures.put(page, new FeaturesImpl(filterDistFs, vec));
          return true;
        });

    queryTerms.forEach(
        term -> {
          if (rareTermsInvIdx.containsKey(term)) {
            rareTermsInvIdx
                .get(term)
                .forEach(
                    pageId -> {
                      LOG.info("Adding all documents with rare query term " + term.text());
                      allFilterFeatures.put(
                          PlainPage.create(pageId, this), new FeaturesForRequiredDocument());
                      return true;
                    });
          }
        });
    return allFilterFeatures;
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
      if (term.lemma() != term) {
        return termStatistics(((IndexTerm) term.lemma()).id()).getDocumentFrequency();
      } else {
        return termStatistics(term.id()).getDocumentFrequency();
      }
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
      try {
        suggestUnigramDb =
            JniDBFactory.factory.open(
                indexRoot.resolve(PlainIndexBuilder.SUGGEST_UNIGRAM_ROOT).toFile(), DEFAULT_DB_OPTIONS);
        suggestMultigramDb =
            JniDBFactory.factory.open(
                indexRoot.resolve(PlainIndexBuilder.SUGGEST_MULTIGRAMS_ROOT).toFile(),
                DEFAULT_DB_OPTIONS);
      } catch (IOException e) {
        e.printStackTrace();
      }

      suggestLoader = new SuggestInformationLoader(suggestUnigramDb, suggestMultigramDb, termBase);
    }
    return suggestLoader;
  }
}
