package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.ml.embedding.Embedding;
import com.expleague.ml.embedding.decomp.DecompBuilder;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta.UriPageMapping;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term.Builder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

public class PlainIndexBuilder implements IndexBuilder {

  public static final int STATISTICS_BLOCK_SIZE = 1 << 10;
  public static final int PLAIN_PAGE_BLOCK_SIZE = 1 << 20;
  public static final int PLAIN_TERM_BLOCK_SIZE = 1 << 20;

  public static final String TERM_STATISTICS_ROOT = "stats";
  public static final String PAGE_ROOT = "page";
  public static final String TERM_ROOT = "term";
  public static final String EMBEDDING_ROOT = "embedding";
  public static final String LSH_METRIC_ROOT = "lsh_metric";
  private static final String JMLL_ROOT = "jmll";

  public static final String SUGGEST_UNIGRAM_ROOT = "suggest/unigram_coeff";
  public static final String SUGGEST_MULTIGRAMS_ROOT = "suggest/multigram_freq_norm";
  public static final String SUGGEST_INVERTED_INDEX_ROOT = "suggest/inverted_index";

  public static final String INDEX_META_FILE = "index.meta";
  public static final int DEFAULT_VEC_SIZE = 130;
  public static final int ROOT_PAGE_ID_OFFSET_BITS = 16;

  private static final int NEIGHBORS_NUM = 50;
  private static final int NUM_OF_RANDOM_IDS = 100;

  private static final int TERM_BATCH_SIZE = 1024;
  private static final WriteOptions DEFAULT_TERM_WRITE_OPTIONS =
      new WriteOptions().sync(true).snapshot(false);

  // DB configurations
  private static final long DEFAULT_CACHE_SIZE = 1 << 10; // 1 KB
  private static final Options STATS_DB_OPTIONS =
      new Options()
          .cacheSize(DEFAULT_CACHE_SIZE)
          .blockSize(STATISTICS_BLOCK_SIZE) // 1 MB
          .createIfMissing(true)
          .errorIfExists(true)
          .compressionType(CompressionType.SNAPPY);
  private static final Options PAGE_DB_OPTIONS =
      new Options()
          .cacheSize(DEFAULT_CACHE_SIZE)
          .blockRestartInterval(PLAIN_PAGE_BLOCK_SIZE)
          .createIfMissing(true)
          .errorIfExists(true)
          .compressionType(CompressionType.SNAPPY);
  private static final Options TERM_DB_OPTIONS =
      new Options()
          .cacheSize(DEFAULT_CACHE_SIZE)
          .blockRestartInterval(PLAIN_TERM_BLOCK_SIZE)
          .createIfMissing(true)
          .errorIfExists(true)
          .compressionType(CompressionType.SNAPPY);

  private static final String[] REQUIRED_WORDS = new String[]{"путин", "медведев", "александр"};

  private static final Logger LOG = Logger.getLogger(PlainIndexBuilder.class.getName());
  private final Crawler crawler;
  private final Config config;
  private final Lemmer lemmer;

  @Inject
  public PlainIndexBuilder(Crawler crawler, Config config, Lemmer lemmer) {
    this.crawler = crawler;
    this.config = config;
    this.lemmer = lemmer;
  }

  // TODO refactor this
  // Yes, this is a dirty copy-paste
  @VisibleForTesting
  static Iterable<UriPageMapping> toProtobufIterableUri(TObjectLongMap<String> mappings) {
    final List<UriPageMapping> protobufMappings = new ArrayList<>();
    final UriPageMapping.Builder uriMappingsBuilder = UriPageMapping.newBuilder();
    mappings.forEachEntry(
        (w, id) -> {
          protobufMappings.add(uriMappingsBuilder.setPageId(id).setUri(w).build());
          return true;
        });

    return protobufMappings;
  }

  /**
   * Converts an array of words to an array of integer ids. Also if a word was not found in mappings
   * the method adds a new entry to mapping for such word
   *
   * @param words array of words to be converted to ids
   * @param mappings all known word to int mappings
   * @return array of word ids in the same order as given words
   */
  @VisibleForTesting
  static long[] toIds(Stream<CharSequence> words, TObjectLongMap<String> mappings) {
    return words
        .map(Object::toString)
        .mapToLong(
            word -> {
              if (!mappings.containsKey(word)) {
                // TODO: currently it generates A LOT of warnings (it's ok but I don't want to se them)
//                LOG.warn(
//                    String.format(
//                        "For the word '%s' was not found any vector representation!", word));
                mappings.put(word, mappings.size() + 1);
              }
              return mappings.get(word);
            })
        .toArray();
  }

  @VisibleForTesting
  static Vec toVector(long[] tokens, TLongObjectMap<Vec> vectors) {
    ArrayVec mean = new ArrayVec(new double[DEFAULT_VEC_SIZE]);
    int vectorsFound = 0;
    for (long i : tokens) {
      if (vectors.containsKey(i)) {
        mean.add((ArrayVec) vectors.get(i));
        ++vectorsFound;
      }
    }
    if (vectorsFound != tokens.length) {
      LOG.warn("");
    }

    mean.scale(vectorsFound == 0 ? 1 : 1.0 / vectorsFound);
    return mean;
  }

  private Comparator<Vec> comparator(Vec main) {
    return (v1, v2) -> {
      double val1 = 1. - VecTools.cosine(v1, main);
      double val2 = 1. - VecTools.cosine(v2, main);
      if (val1 < val2) {
        return -1;
      } else if (val1 > val2) {
        return 1;
      }
      return 0;
    };
  }

  private Collection<Long> nearest(TLongObjectMap<Vec> vecs, long mainId) {
    Vec mainVec = vecs.get(mainId);
    if (mainVec == null) {
      return new ArrayList<>();
    }
    Comparator<Vec> comparator = comparator(mainVec);
    TreeMap<Vec, Long> neighbors = new TreeMap<>(comparator);
    vecs.forEachEntry(
        (id, vec) -> {
          if (neighbors.size() < NEIGHBORS_NUM) {
            neighbors.put(vec, id);
          } else if (comparator.compare(neighbors.lastKey(), vec) > 0) {
            neighbors.remove(neighbors.lastKey());
            neighbors.put(vec, id);
          }
          return true;
        });
    return neighbors.values();
  }

  private void saveIds(Path root, TLongObjectMap<Vec> vecs, Set<Long> ids) throws IOException {
    for (long mainId : ids) {
      try (Writer out =
          new OutputStreamWriter(new FileOutputStream(root.resolve(mainId + "").toFile()))) {
        for (long id : nearest(vecs, mainId)) {
          out.write(id + " ");
        }
      }
    }
  }

  private void saveLSHMetricInfo(Path root, TLongObjectMap<Vec> vecs, Set<Long> requiredIds)
      throws IOException {
    saveIds(root, vecs, requiredIds);
    Random random = new Random();
    Set<Long> randomIds = new HashSet<>();
    for (int i = 0; i < NUM_OF_RANDOM_IDS; i++) {
      long mainId;
      do {
        mainId = random.nextInt(vecs.size());
      } while (requiredIds.contains(mainId));
      randomIds.add(mainId);
    }
    saveIds(root, vecs, randomIds);
  }

  @Deprecated
  @VisibleForTesting
  static void readGloveVectors(
      Path glovePath, TObjectLongMap<String> idMappings, TLongObjectMap<Vec> vectors) {
    try (Reader input = new InputStreamReader(new FileInputStream(glovePath.toFile()))) {

      CharSeqTools.lines(input)
          .parallel()
          .forEach(
              line -> {
                String[] tokens = line.toString().split("\\s");
                final String word = tokens[0].toLowerCase();
                final int dim = Integer.parseInt(tokens[1]);

                if (dim != DEFAULT_VEC_SIZE) {
                  throw new IllegalArgumentException(
                      "Wrong vectors dim:  expected " + DEFAULT_VEC_SIZE + ", found " + dim);
                }

                double[] doubles =
                    Arrays.stream(tokens, 2, tokens.length)
                        .mapToDouble(CharSeqTools::parseDouble)
                        .toArray();
                synchronized (idMappings) {
                  if (idMappings.containsKey(word)) {
                    throw new IllegalArgumentException("Embedding contains duplicate words!");
                  }
                  idMappings.put(word, idMappings.size() + 1);
                }
                synchronized (vectors) {
                  vectors.put(idMappings.get(word), new ArrayVec(doubles));
                }
              });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void buildPage(Path indexRoot, List<CrawlerDocument> docs) throws IOException {
      Path pagePath = indexRoot.resolve(PAGE_ROOT);
      if (!Files.exists(pagePath)) {
          LOG.info("Build page...");
          Files.createDirectories(pagePath);
          try (final DB pageDb =
                       JniDBFactory.factory.open(indexRoot.resolve(PAGE_ROOT).toFile(), PAGE_DB_OPTIONS)
          ) {
              final PlainPageBuilder plainPageBuilder =
                      new PlainPageBuilder(pageDb, indexRoot.resolve(PAGE_ROOT).resolve("TMP"));
              long[] pagesCount = new long[]{0};
              long[] sectionId = new long[]{0};
              for (CrawlerDocument doc : docs) {
                  long rootPageId = -((pagesCount[0] + 1) << ROOT_PAGE_ID_OFFSET_BITS);
                  sectionId[0] = rootPageId;
                  plainPageBuilder.startPage(doc.id(), rootPageId, doc.categories());
                  doc.sections()
                          .forEachOrdered(
                                  s -> {
                                      plainPageBuilder.addSection(sectionId[0], s);
                                      //                          embeddingBuilder.add(sectionId[0],
                                      // toVector(titleIds, gloveVectors));
                                      --sectionId[0];
                                  });
                  plainPageBuilder.endPage();
                  ++pagesCount[0];
              }
              plainPageBuilder.build();
          }
      } else {
          LOG.info("Page exists");
      }
  }

  private void buildTermStatistics(Path indexRoot, List<CrawlerDocument> docs, TObjectLongMap<String> idMappings) throws IOException {
      Path termStatisticsPath = indexRoot.resolve(TERM_STATISTICS_ROOT);
      if (!Files.exists(termStatisticsPath)) {
          LOG.info("Build stat...");
          Files.createDirectories(termStatisticsPath);
          try (final DB statisticsDb =
                  JniDBFactory.factory.open(
                          indexRoot.resolve(TERM_STATISTICS_ROOT).toFile(), STATS_DB_OPTIONS)
          ) {
              final StatisticsBuilder statisticsBuilder = new StatisticsBuilder(statisticsDb);
              for (CrawlerDocument doc : docs) {
                  TLongList pageTokens = new TLongArrayList();
                  doc.sections()
                          .forEachOrdered(
                                  s -> {
                                      List<CharSequence> sectionTitles = s.title();
                                      String sectionTitle =
                                              sectionTitles.get(sectionTitles.size() - 1).toString();
                                      long[] titleIds =
                                              toIds(
                                                      tokenizer.parseTextToWords(sectionTitle.toLowerCase()),
                                                      idMappings);
                                      //                          embeddingBuilder.add(sectionId[0],
                                      // toVector(titleIds, gloveVectors));

                                      pageTokens.addAll(titleIds);
                                      pageTokens.addAll(
                                              toIds(
                                                      tokenizer.parseTextToWords(s.text().toString().toLowerCase()),
                                                      idMappings));
                                  });
                  statisticsBuilder.enrich(pageTokens, null);
              }
              statisticsBuilder.build();
          }
      } else {
          LOG.info("Stat exists");
      }
  }

  private void buildSuggest(Path indexRoot, List<CrawlerDocument> docs, TObjectLongMap<String> idMappings) throws IOException {
      Path suggestParh = indexRoot.resolve("suggest");
      if (!Files.exists(suggestParh)) {
          LOG.info("Build suggest...");
          Files.createDirectories(indexRoot.resolve(SUGGEST_UNIGRAM_ROOT));
          try (final DB suggest_unigram_DB =
                       JniDBFactory.factory.open(
                               indexRoot.resolve(SUGGEST_UNIGRAM_ROOT).toFile(), STATS_DB_OPTIONS);
               final DB suggest_multigram_DB =
                       JniDBFactory.factory.open(
                               indexRoot.resolve(SUGGEST_MULTIGRAMS_ROOT).toFile(), STATS_DB_OPTIONS);
               final DB suggest_inverted_index_DB =
                       JniDBFactory.factory.open(
                               indexRoot.resolve(SUGGEST_INVERTED_INDEX_ROOT).toFile(), STATS_DB_OPTIONS)) {
              final SuggestInformationBuilder suggestBuilder =
                      new SuggestInformationBuilder(
                              suggest_unigram_DB, suggest_multigram_DB, suggest_inverted_index_DB);
              for (CrawlerDocument doc : docs) {
                  long[] titleTokens = toIds(
                          tokenizer.parseTextToWords(doc.title().toLowerCase()),
                          idMappings);
                  suggestBuilder.accept(titleTokens);
              }
              suggestBuilder.build();
          }
      } else {
          LOG.info("Suggest exists");
      }
  }

  private void buildJmll(Path indexRoot, List<CrawlerDocument> docs, TObjectLongMap<String> idMappings, TLongObjectMap<Vec> vectors) throws IOException {
      Path jmllPath = indexRoot.resolve(JMLL_ROOT);
      if (!Files.exists(jmllPath)) {
          LOG.info("Build jmll...");
          Files.createDirectories(jmllPath);
          Path corpus = jmllPath.resolve("corpus");
          try (Writer to = new OutputStreamWriter(new FileOutputStream(corpus.toFile()))) {
              for (CrawlerDocument doc : docs) {
                  try {
                      String title = doc.title();
                      for (char c : title.toCharArray()) {
                          to.write(filtrateChar(c));
                      }
                      to.write(' ');
                      CharSequence content = doc.content();
                      for (int i = 0; i < content.length(); i++) {
                          to.write(filtrateChar(content.charAt(i)));
                      }
                      to.write(' ');
                  } catch (IOException e) {
                      LOG.warn(e);
                  }
              }
          }
          DecompBuilder builder = (DecompBuilder) Embedding.builder(Embedding.Type.DECOMP);
          final Embedding<CharSeq> jmllEmbedding = builder.file(corpus).build();
          try (Writer to = Files.newBufferedWriter(jmllPath.resolve("vecs"))) {
              Embedding.write(jmllEmbedding, to);
          }
          idMappings.forEachEntry((word, id) -> {
              vectors.put(id, jmllEmbedding.apply(CharSeq.create(word)));
              return true;
          });
      } else {
          LOG.info("Jmll exists");
          //TODO: read vectors
      }
  }

  private void buildEmbedding(Path indexRoot, List<CrawlerDocument> docs, TObjectLongMap<String> idMappings, TLongObjectMap<Vec> vectors) throws IOException {
      Path embeddingPath = indexRoot.resolve(EMBEDDING_ROOT);
      if (!Files.exists(embeddingPath)) {
          LOG.info("Build embedding");
          final EmbeddingBuilder embeddingBuilder = new EmbeddingBuilder(embeddingPath);
          long[] pagesCount = new long[]{0};
          embeddingBuilder.addAll(vectors);
          for (CrawlerDocument doc : docs) {
              long rootPageId = -((pagesCount[0] + 1) << ROOT_PAGE_ID_OFFSET_BITS);
              long[] titleIds = toIds(tokenizer.parseTextToWords(doc.title()), idMappings);
              embeddingBuilder.add(rootPageId, toVector(titleIds, vectors));
              ++pagesCount[0];
          }
          embeddingBuilder.build();
          Path lshMetricPath = embeddingPath.resolve(LSH_METRIC_ROOT);
          Files.createDirectories(lshMetricPath);
          saveLSHMetricInfo(
                  lshMetricPath,
                  vectors,
                  Arrays.stream(REQUIRED_WORDS).map(idMappings::get).collect(Collectors.toSet()));
      } else {
          LOG.info("Embedding exists");
      }
  }

  private void buildTerm(Path indexRoot, TObjectLongMap<String> idMappings) throws IOException {
      Path termPath = indexRoot.resolve(TERM_ROOT);
      if (!Files.exists(termPath)) {
          LOG.info("Build term...");
          Files.createDirectories(indexRoot.resolve(TERM_ROOT));
          TLongObjectMap<ParsedTerm> idToTermMapping = parseTerms(idMappings, lemmer);
          try (DB termDb =
                       JniDBFactory.factory.open(indexRoot.resolve(TERM_ROOT).toFile(), TERM_DB_OPTIONS)) {

              WriteBatch[] batch = new WriteBatch[]{termDb.createWriteBatch()};
              int[] curBatchSize = new int[]{0};

              idMappings.forEachEntry(
                      (word, id) -> {
                          Builder termBuilder = Term.newBuilder().setId(id).setText(word);
                          ParsedTerm term = idToTermMapping.get(id);
                          if (term != null) {
                              if (term.lemmaId != -1) {
                                  termBuilder.setLemmaId(term.lemmaId);
                              }
                              if (term.partOfSpeech != null) {
                                  termBuilder.setPartOfSpeech(
                                          IndexUnits.Term.PartOfSpeech.valueOf(term.partOfSpeech.name()));
                              }
                          }

                          batch[0].put(Longs.toByteArray(id), termBuilder.build().toByteArray());
                          curBatchSize[0]++;
                          if (curBatchSize[0] >= TERM_BATCH_SIZE) {
                              termDb.write(batch[0], DEFAULT_TERM_WRITE_OPTIONS);

                              batch[0] = termDb.createWriteBatch();
                              curBatchSize[0] = 0;
                          }

                          return true;
                      });

              if (curBatchSize[0] > 0) {
                  termDb.write(batch[0], DEFAULT_TERM_WRITE_OPTIONS);
              }
          }
      } else {
          LOG.info("Term exists");
      }
  }

  private void saveIndexMeta(Path indexRoot, List<CrawlerDocument> docs, TObjectLongMap<String> idMappings) throws IOException {
      LOG.info("Storing index meta...");
      long[] pagesCount = new long[]{0};
      final long[] pagesAndTokensCounts = new long[]{0, 0};
      final TObjectLongMap<String> uriPageIdMapping = new TObjectLongHashMap<>();
      for (CrawlerDocument doc : docs) {
          long rootPageId = -((pagesCount[0] + 1) << ROOT_PAGE_ID_OFFSET_BITS);
          TLongList pageTokens = new TLongArrayList();
          doc.sections()
                  .forEachOrdered(
                          s -> {
                              List<CharSequence> sectionTitles = s.title();
                              String sectionTitle =
                                      sectionTitles.get(sectionTitles.size() - 1).toString();
                              long[] titleIds =
                                      toIds(
                                              tokenizer.parseTextToWords(sectionTitle.toLowerCase()),
                                              idMappings);
                              //                          embeddingBuilder.add(sectionId[0],
                              // toVector(titleIds, gloveVectors));

                              pageTokens.addAll(titleIds);
                              pageTokens.addAll(
                                      toIds(
                                              tokenizer.parseTextToWords(s.text().toString().toLowerCase()),
                                              idMappings));
                          });
          ++pagesCount[0];
          ++pagesAndTokensCounts[0];
          pagesAndTokensCounts[1] += pageTokens.size();
          uriPageIdMapping.put(doc.uri().toString(), rootPageId);
          try {
              uriPageIdMapping.put(
                      URLDecoder.decode(doc.uri().toString(), "UTF-8"), rootPageId);
          } catch (UnsupportedEncodingException e) {
              LOG.warn(e);
          }
      }
      IndexUnits.IndexMeta.newBuilder()
              .setVersion(PlainIndex.VERSION)
              .setAveragePageSize((double) pagesAndTokensCounts[1] / pagesAndTokensCounts[0])
              .setVocabularySize(idMappings.size())
              .addAllUriPageMappings(toProtobufIterableUri(uriPageIdMapping))
              .setPagesCount((int) pagesAndTokensCounts[0])
              .build()
              .writeTo(Files.newOutputStream(indexRoot.resolve(INDEX_META_FILE)));
  }

  private final Tokenizer tokenizer = new TokenizerImpl();

  //TODO: remove duplicate code
  @Override
  public void buildIndex() throws IOException {
      LOG.info("Building started...");
      try {
          final Path indexRoot = config.getTemporaryIndex();
          final TObjectLongMap<String> idMappings = new TObjectLongHashMap<>();
          final TLongObjectMap<Vec> vectors = new TLongObjectHashMap<>();

          List<CrawlerDocument> docs = crawler.makeStream().collect(Collectors.toList());
          for (CrawlerDocument doc : docs) {
              doc.sections()
                      .forEachOrdered(
                              s -> {
                                  List<CharSequence> sectionTitles = s.title();
                                  String sectionTitle = sectionTitles.get(sectionTitles.size() - 1).toString();
                                  toIds(tokenizer.parseTextToWords(sectionTitle.toLowerCase()), idMappings);
                                  toIds(tokenizer.parseTextToWords(s.text().toString().toLowerCase()), idMappings);
                              });
              toIds(tokenizer.parseTextToWords(doc.title().toLowerCase()), idMappings);
          }

          buildPage(indexRoot, docs);
          buildTermStatistics(indexRoot, docs, idMappings);
          buildSuggest(indexRoot, docs, idMappings);
          buildJmll(indexRoot, docs, idMappings, vectors);
          buildEmbedding(indexRoot, docs, idMappings, vectors);
          buildTerm(indexRoot, idMappings);
          saveIndexMeta(indexRoot, docs, idMappings);
      } catch (Exception e) {
          throw new RuntimeException(e);
      }
      LOG.info("Index built!");
  }

  private char filtrateChar(char c) {
      if (Character.isLetter(c)) {
          return Character.toLowerCase(c);
      } else if (Character.isDigit(c)) {
          return c;
      }
      return ' ';
  }

  @VisibleForTesting
  static TLongObjectMap<ParsedTerm> parseTerms(TObjectLongMap<String> idMapping, Lemmer lemmer) {
    MyStem stem = lemmer.myStem;
    TLongObjectMap<ParsedTerm> terms = new TLongObjectHashMap<>();
    TObjectLongMap<String> newIds = new TObjectLongHashMap<>();

    idMapping.forEachKey(
        word -> {
          final List<WordInfo> parse = stem.parse(word);
          final LemmaInfo lemma = parse.size() > 0 ? parse.get(0).lemma() : null;
          final long wordId = idMapping.get(word);

          if (lemma == null || lemma.lemma().equals(word)) {
            terms.put(
                wordId,
                new ParsedTerm(
                    wordId, -1, lemma == null ? null : PartOfSpeech.valueOf(lemma.pos().name())));
            return true;
          }

          long lemmaId;

          String lemmaStr = lemma.lemma().toString();
          if (idMapping.containsKey(lemmaStr)) {
            lemmaId = idMapping.get(lemmaStr);
          } else if (newIds.containsKey(lemmaStr)) {
            lemmaId = newIds.get(lemmaStr);
          } else {
            lemmaId = idMapping.size() + newIds.size() + 1;
            newIds.put(lemma.lemma().toString(), lemmaId);
          }

          terms.put(
              wordId, new ParsedTerm(wordId, lemmaId, PartOfSpeech.valueOf(lemma.pos().name())));
          if (!terms.containsKey(lemmaId)) {
            terms.put(
                lemmaId, new ParsedTerm(lemmaId, -1, PartOfSpeech.valueOf(lemma.pos().name())));
          }

          return true;
        });

    idMapping.putAll(newIds);
    return terms;
  }

  static class ParsedTerm {

    long id;
    long lemmaId;
    PartOfSpeech partOfSpeech;

    public ParsedTerm(long id, long lemmaId, PartOfSpeech partOfSpeech) {
      this.id = id;
      this.lemmaId = lemmaId;
      this.partOfSpeech = partOfSpeech;
    }
  }
}
