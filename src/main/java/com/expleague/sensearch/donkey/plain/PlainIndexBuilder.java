package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.ml.embedding.Embedding;
import com.expleague.ml.embedding.impl.EmbeddingImpl;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.core.Annotations.EmbeddingVectorsPath;
import com.expleague.sensearch.core.Annotations.IndexRoot;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import com.expleague.sensearch.core.lemmer.Lemmer;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.plain.IndexMetaBuilder.TermSegment;
import com.expleague.sensearch.donkey.utils.BrandNewIdGenerator;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.web.suggest.SuggestInformationBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import gnu.trove.list.TLongList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;

public class PlainIndexBuilder implements IndexBuilder {

  public static final String URI_MAPPINGS_ROOT = "UriMappings";
  public static final String TERM_STATISTICS_ROOT = "TermStatistics";
  public static final String PAGE_BASE_ROOT = "PageBase";
  public static final String TERM_BASE_ROOT = "TermBase";
  public static final String EMBEDDING_ROOT = "Embeddings";
  public static final String LSH_METRIC_ROOT = "lsh_metric";
  public static final String LSH_ROOT = "lsh";
  public static final String TEMP_EMBEDDING_ROOT = "temp_embedding";
  public static final String VECS_ROOT = "vecs";

  public static final Path RARE_INV_IDX_FILE = Paths.get("rare_iidx").resolve("iidx");
  
  public static final String SUGGEST_UNIGRAM_ROOT = "suggest/unigram_coeff";
  public static final String SUGGEST_MULTIGRAMS_ROOT = "suggest/multigram_freq_norm";

  public static final String INDEX_META_FILE = "index.meta";
  public static final int DEFAULT_VEC_SIZE = 130;

  private static final int NEIGHBORS_NUM = 50;
  private static final int NUM_OF_RANDOM_IDS = 100;

  // DB configurations

  private static final String[] REQUIRED_WORDS = new String[]{"путин", "медведев", "александр"};

  private static final Logger LOG = Logger.getLogger(PlainIndexBuilder.class);
  private final Crawler crawler;
  private final Lemmer lemmer;
  private final Tokenizer tokenizer = new TokenizerImpl();
  private final Path indexRoot;
  private final Path embeddingVectorsPath;

  private final Map<String, ParsedTerm> termsCache = new HashMap<>();

  @Inject
  public PlainIndexBuilder(
      Crawler crawler,
      @IndexRoot Path indexRoot,
      @EmbeddingVectorsPath Path embeddingVectorsPath,
      Lemmer lemmer) {
    this.crawler = crawler;
    this.indexRoot = indexRoot;
    this.embeddingVectorsPath = embeddingVectorsPath;

    this.lemmer = lemmer;
  }

  /**
   * Tokenizes given text and adds it to the @code{termBuilder}.
   *
   * @param text text to be parsed and converted to ids
   * @return array of term ids returned by @code{termBuilder}
   */
  private long[] toTermIds(String text, TermBuilder termBuilder) {
    return tokenizer
        .parseTextToWords(text)
        .map(s -> s.toString().toLowerCase())
        .map(word -> termBuilder.addTerm(word).id)
        .mapToLong(i -> i)
        .toArray();
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
          new OutputStreamWriter(new FileOutputStream(root.resolve("_" + mainId).toFile()))) {
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

  @Override
  public void buildIndexAndEmbedding() throws IOException {
    LOG.info("Building JMLL embedding...");
    EmbeddingImpl<CharSeq> jmllEmbedding;
    jmllEmbedding =
        (EmbeddingImpl<CharSeq>)
            new JmllEmbeddingBuilder(DEFAULT_VEC_SIZE, indexRoot.resolve(TEMP_EMBEDDING_ROOT))
                .build(crawler.makeStream());
    jmllEmbedding.write(new FileWriter(embeddingVectorsPath.toFile()));
    buildIndex(jmllEmbedding);
  }

  // TODO: Make it more readable, add possibility of incomplete rebuilding
  @Override
  public void buildIndex() throws IOException {
    LOG.info("Reading jmll embedding...");
    buildIndex(
        EmbeddingImpl.read(
            Files.newBufferedReader(embeddingVectorsPath, Charset.forName("UTF-8")),
            CharSeq.class));
  }

  private void buildIndex(Embedding<CharSeq> jmllEmbedding) throws IOException {
    long startTime = System.nanoTime();
    buildIndexInternal(jmllEmbedding);
    buildSuggestAfterIndex();
    LOG.info(String.format("Index build in [%.3f] seconds",
        (System.nanoTime() - startTime) / 1e9));
  }

  public void buildSuggestAfterIndex() throws IOException {

    Files.createDirectories(indexRoot.resolve(SUGGEST_UNIGRAM_ROOT));

    try (final DB suggestUnigramDb =
        JniDBFactory.factory.open(
            indexRoot.resolve(SUGGEST_UNIGRAM_ROOT).toFile(), STATS_DB_OPTIONS);
        final DB suggestMultigramDb =
            JniDBFactory.factory.open(
                indexRoot.resolve(SUGGEST_MULTIGRAMS_ROOT).toFile(), STATS_DB_OPTIONS);
        ) {
      Config config =
          new ObjectMapper().readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);
      Injector injector = Guice.createInjector(new AppModule(config));

      try (Index index = injector.getInstance(Index.class)) {

        LOG.info("Building suggest...");
        long start = System.nanoTime();
        SuggestInformationBuilder suggestBuilder =
            new SuggestInformationBuilder(index, config.getIndexRoot(),
                suggestUnigramDb, suggestMultigramDb);

        suggestBuilder.build();


        LOG.info(String.format("Suggest index was built in %.1f sec",
            (System.nanoTime() - start) / 1e9));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }


  private void buildIndexInternal(Embedding<CharSeq> jmllEmbedding) throws IOException {
    LOG.info("Creating database files...");
    final TLongSet knownPageIds = new TLongHashSet();
    try (
        final PlainPageBuilder plainPageBuilder =
            new PlainPageBuilder(indexRoot.resolve(PAGE_BASE_ROOT));
        final TermBuilder termBuilder =
            new TermBuilder(indexRoot.resolve(TERM_BASE_ROOT));
        final StatisticsBuilder statisticsBuilder =
            new StatisticsBuilder(indexRoot.resolve(TERM_BASE_ROOT));
        final UriMappingsBuilder uriMappingBuilder =
            new UriMappingsBuilder(indexRoot.resolve(URI_MAPPINGS_ROOT));
        final EmbeddingBuilder embeddingBuilder =
            new EmbeddingBuilder(
                JniDBFactory.factory.open(
                    indexRoot.resolve(EMBEDDING_ROOT).resolve(VECS_ROOT).toFile(),
                    EMBEDDING_DB_OPTIONS),
                /*JniDBFactory.factory.open(
                    indexRoot.resolve(EMBEDDING_ROOT).resolve(LSH_ROOT).toFile(),
                    EMBEDDING_DB_OPTIONS),
                indexRoot.resolve(EMBEDDING_ROOT),*/
                jmllEmbedding,
                tokenizer)) {

      EmbeddingImpl<CharSeq> jmllEmbedding1 = (EmbeddingImpl<CharSeq>) jmllEmbedding;
      for (int i = 0; i < jmllEmbedding1.vocabSize(); i++) {
        ParsedTerm parsedTerm = termBuilder.addTerm(jmllEmbedding1.getObj(i));
        embeddingBuilder.addTerm(parsedTerm.id, jmllEmbedding1.apply(parsedTerm.text));
      }
      IndexMetaBuilder indexMetaBuilder = new IndexMetaBuilder(PlainIndex.VERSION);

      LOG.info("Creating mappings from wiki ids to raw index ids...");

      final TLongObjectMap<TLongList> rareTermsInvIdx = new TLongObjectHashMap<>();
      
      try {
        LOG.info("Parsing pages...");
        BrandNewIdGenerator idGenerator = BrandNewIdGenerator.getInstance();
        int[] docCnt = {0};
        crawler
            .makeStream()
            .filter(Objects::nonNull)
            .forEach(
                doc -> {                  
                  docCnt[0]++;
                  if (docCnt[0] % 10_000 == 0) {
                    LOG.debug(docCnt[0] + " documents processed...");
                  }
                  long pageId = idGenerator.generatePageId(doc.uri());
                  // We don't add uri to the knownPageIds as we need first section to have the
                  // same Id
                  // knownPageIds.add(uri);
                  plainPageBuilder.startPage(pageId, doc.categories(), doc.uri());
                  statisticsBuilder.startPage();
                  indexMetaBuilder.startPage(
                      pageId, (int) tokenizer.parseTextToWords(doc.title()).count());
                  embeddingBuilder.startPage(pageId);

                  doc.sections()
                      .forEachOrdered(
                          s -> {
                            long sectionId = idGenerator.generatePageId(s.uri());
                            knownPageIds.add(sectionId);

                            s.links().forEach(indexMetaBuilder::addLink);

                            plainPageBuilder.addSection(s, sectionId);
                            indexMetaBuilder.addSection(sectionId);
                            embeddingBuilder.addSection(s, sectionId);
                            uriMappingBuilder.addSection(s.uri(), sectionId);

                            List<CharSequence> sectionTitles = s.titles();
                            String sectionTitle =
                                sectionTitles.get(sectionTitles.size() - 1).toString();

                            final CharSequence TITLE_STOP = "@@@STOP_TITLE777@@@";
                            boolean[] isTitle = {true};
                            Stream.concat(
                                Stream.concat(
                                    tokenizer.parseTextToWords(sectionTitle),
                                    Stream.of(TITLE_STOP)),
                                tokenizer.parseTextToWords(s.text()))
                                .map(CharSeqTools::toLowerCase)
                                .forEach(
                                    word -> {
                                      if (word == TITLE_STOP) {
                                        isTitle[0] = false;
                                      }
                                      TermBuilder.ParsedTerm termLemmaId =
                                          termBuilder.addTerm(word);

                                      // TODO: uncomment it
                                      /*
                                      if (jmllEmbedding.apply(CharSeq.compact(word)) == null) {
                                        rareTermsInvIdx
                                            .putIfAbsent(termLemmaId.id, new TLongArrayList());
                                        rareTermsInvIdx.get(termLemmaId.id).add(pageId);
                                      }
                                      */
                                      
                                      long lemmaId =
                                          termLemmaId.lemmaId == -1
                                              ? termLemmaId.id
                                              : termLemmaId.lemmaId;
                                      statisticsBuilder.enrich(termLemmaId.id, lemmaId);
                                      indexMetaBuilder.addTerm(
                                          termLemmaId.id,
                                          isTitle[0]
                                              ? TermSegment.SECTION_TITLE
                                              : TermSegment.TEXT);
                                    });
                          });

                  embeddingBuilder.endPage();
                  indexMetaBuilder.endPage();
                  statisticsBuilder.endPage();
                  plainPageBuilder.endPage();
                });

        Path lshMetricPath = indexRoot.resolve(EMBEDDING_ROOT).resolve(LSH_METRIC_ROOT);
        Files.createDirectories(lshMetricPath);

        // TODO: uncomment this
        //        saveLSHMetricInfo(
        //            lshMetricPath,
        //            gloveVectors,
        //
        // Arrays.stream(REQUIRED_WORDS).map(idMappings::get).collect(Collectors.toSet()));

        LOG.info("Storing index meta...");
        // saving index-wise data
        indexMetaBuilder.build().writeTo(Files.newOutputStream(indexRoot.resolve(INDEX_META_FILE)));

        {
          ObjectMapper mapper = new ObjectMapper();
          Files.createDirectories(indexRoot.resolve(RARE_INV_IDX_FILE).getParent());
          mapper.writeValue(indexRoot.resolve(RARE_INV_IDX_FILE).toFile(), rareTermsInvIdx);
        }

      } catch (Exception e) {
        FileUtils.deleteDirectory(indexRoot.resolve(PAGE_BASE_ROOT).toFile());
        FileUtils.deleteDirectory(indexRoot.resolve(TERM_STATISTICS_ROOT).toFile());
        FileUtils.deleteDirectory(indexRoot.resolve(EMBEDDING_ROOT).toFile());
        FileUtils.deleteDirectory(indexRoot.resolve("suggest").toFile());
        FileUtils.deleteDirectory(indexRoot.resolve(TERM_BASE_ROOT).toFile());

        throw new IOException(e);
      }
    }
    LOG.info("Index built!");
  }

  static class ParsedTerm {
    final long wordId;
    final CharSeq word;

    final long lemmaId;
    final CharSeq lemma;

    final PartOfSpeech posTag;

    private ParsedTerm(long wordId, CharSeq word,
        long lemmaId, CharSeq lemma,
        PartOfSpeech posTag) {
      this.wordId = wordId;
      this.word = word;
      this.lemmaId = lemmaId;
      this.lemma = lemma;
      this.posTag = posTag;
    }

    static ParsedTerm parse(CharSequence wordcs, Lemmer lemmer) {
      CharSeq word = CharSeq.create(wordcs);
      word = CharSeq.intern(word);

      LemmaInfo lemma = null;
      List<WordInfo> parse = lemmer.parse(word);
      if (parse.size() > 0) {
        lemma = parse.get(0).lemma();
      }

      long wordId = generateTermId(word);
      if (lemma == null) {
        return new ParsedTerm(wordId, word, -1, null, null);
      }

      long lemmaId = lemma.lemma().equals(word) ? 0 : generateTermId(lemma.lemma());
      return new ParsedTerm(wordId, word, lemmaId, lemma.lemma(),
          PartOfSpeech.valueOf(lemma.pos().name()));
    }

    String lemma() {
      return lemma.toString();
    }

    long lemmaId() {
      return lemmaId;
    }

    String word() {
      return word.toString();
    }

    long wordId() {
      return wordId;
    }

    @Nullable
    PartOfSpeech posTag() {
      return posTag;
    }

    boolean hasLemma() {
      return lemmaId != -1;
    }
  }
}
