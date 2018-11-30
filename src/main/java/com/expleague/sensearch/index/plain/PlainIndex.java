package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.MyStemImpl;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.index.Embedding;
import com.expleague.sensearch.index.Filter;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta.IdMapping;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta.UriPageMapping;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics.TermFrequency;
import com.expleague.sensearch.query.Query;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.apache.log4j.Logger;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.*;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.log4j.Logger;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.ReadOptions;
import org.jetbrains.annotations.Nullable;

public class PlainIndex implements Index {
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
  private final DB plainBase;

  private final double averagePageSize;
  private final int indexSize;
  private final int vocabularySize;

  private final BloomFilter<byte[]> titlesBloomFilter;

  private final Embedding embedding;
  private final Filter filter;
  private final MyStem stemmer;
  private final Tokenizer tokenizer;

  private TermStatistics lastTermStatistics;

  public PlainIndex(Config config) throws IOException {
    indexRoot = config.getTemporaryIndex();

    embedding = new EmbeddingImpl(indexRoot.resolve(PlainIndexBuilder.EMBEDDING_ROOT));
    filter = new FilterImpl(embedding);

    termStatisticsBase =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.TERM_STATISTICS_ROOT).toFile(), DEFAULT_DB_OPTIONS);

    plainBase =
        JniDBFactory.factory.open(
            indexRoot.resolve(PlainIndexBuilder.PLAIN_ROOT).toFile(), DEFAULT_DB_OPTIONS);

    tokenizer = new TokenizerImpl();

    stemmer = new MyStemImpl(config.getMyStem());

    IndexUnits.IndexMeta indexMeta =
        IndexUnits.IndexMeta.parseFrom(
            Files.newInputStream(indexRoot.resolve(PlainIndexBuilder.INDEX_META_FILE)));
    averagePageSize = indexMeta.getAveragePageSize();
    indexSize = indexMeta.getPagesCount();
    vocabularySize = indexMeta.getVocabularySize();

    ByteString byteStringFilter = indexMeta.getTitlesBloomFilter();
    titlesBloomFilter =
        BloomFilter.readFrom(
            new ByteArrayInputStream(byteStringFilter.toByteArray(), 0, byteStringFilter.size()),
            Funnels.byteArrayFunnel());

    final TObjectLongMap<CharSeq> ids = new TObjectLongHashMap<>();
    indexMeta.getIdMappingsList().forEach(m -> ids.put(CharSeq.create(m.getWord()), m.getId()));
    for (IdMapping idMapping : indexMeta.getIdMappingsList()) {
      //noinspection SuspiciousMethodCalls
      if (wordToTerms.containsKey(idMapping.getWord()))
        continue;

      final String word = idMapping.getWord();
      final List<WordInfo> parse = stemmer.parse(word);
      final LemmaInfo lemma = parse.size() > 0 ? parse.get(0).lemma() : null;
//      if (lemma == null)
//        System.out.println();
      final IndexTerm lemmaTerm;
      //noinspection EqualsBetweenInconvertibleTypes
      if (lemma == null || lemma.lemma().equals(word)) {
        lemmaTerm = null;
      }
      else if (wordToTerms.containsKey(lemma.lemma())) {
        lemmaTerm = (IndexTerm) wordToTerms.get(lemma.lemma());
      }
      else {
        long lemmaId = ids.get(lemma.lemma());
        if (lemmaId == ids.getNoEntryValue()) {
          lemmaId = ids.size();
          ids.put(lemma.lemma(), lemmaId);
        }
        CharSeq compactLemma = CharSeq.intern(lemma.lemma());
        lemmaTerm = new IndexTerm(this, compactLemma, lemmaId, null);
      }
      final CharSeq compactText = CharSeq.intern(word);
      wordToTerms.put(compactText, new IndexTerm(this, compactText, idMapping.getId(), lemmaTerm));
    }
    wordToTerms.values().forEach(t -> idToTerm.put(((IndexTerm) t).id(), t));

    for (UriPageMapping mapping : indexMeta.getUriPageMappingsList()) {
      uriToPageIdMap.put(URI.create(mapping.getUri()), mapping.getPageId());
    }
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
      return termStatistics(((IndexTerm)term).id()).getBigramFrequencyList().stream().mapToLong(TermFrequency::getTermId).mapToObj(idToTerm::get);
    } catch (InvalidProtocolBufferException e) {
      LOG.warn(
          String.format(
              "Encountered invalid protobuf in statistics base for word with id %d", term.text().toString()));
      return Stream.empty();
    }
  }

  /**
   * TODO: What kind of terms is returned?
   */
  Stream<Term> synonyms(Term term) {
    return filter
        .filtrate(
            embedding.vec(((IndexTerm) term).id()),
            SYNONYMS_COUNT,
            PlainIndex::isWordId)
        .mapToObj(idToTerm::get);
  }

  @Nullable
  private IndexedPage idToPage(long id) {
    try {
      return new PlainPage(IndexUnits.Page.parseFrom(plainBase.get(Longs.toByteArray(id))));
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
    query.terms().stream()
        .mapToLong(t -> ((IndexTerm) t).id())
        .mapToObj(embedding::vec)
        .forEach(v -> VecTools.append(queryVec, v));
    return filter.filtrate(queryVec, FILTERED_DOC_NUMBER, PlainIndex::isPageId).mapToObj(this::idToPage);
  }

  @Override
  public Term term(CharSequence seq) {
    final CharSequence normalized = CharSeqTools.toLowerCase(CharSeqTools.trim(seq));
    return wordToTerms.get(CharSeq.create(normalized));
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
      return (int)termStatistics(((IndexTerm) term).id()).getTermFrequency();
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
