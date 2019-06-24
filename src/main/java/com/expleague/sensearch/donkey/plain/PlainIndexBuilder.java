package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.ml.embedding.Embedding;
import com.expleague.ml.embedding.impl.EmbeddingImpl;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.core.Annotations.EmbeddingVectorsPath;
import com.expleague.sensearch.core.Annotations.IndexRoot;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import com.expleague.sensearch.core.lemmer.Lemmer;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.plain.IndexMetaBuilder.TermSegment;
import com.expleague.sensearch.donkey.randomaccess.LevelDbBasedIndex;
import com.expleague.sensearch.donkey.randomaccess.PageIndex;
import com.expleague.sensearch.donkey.readers.LinkReader;
import com.expleague.sensearch.donkey.readers.Reader;
import com.expleague.sensearch.donkey.utils.BrandNewIdGenerator;
import com.expleague.sensearch.donkey.utils.Dictionary;
import com.expleague.sensearch.donkey.utils.ParsedTerm;
import com.expleague.sensearch.donkey.utils.TokenParser;
import com.expleague.sensearch.donkey.writers.LinkWriter;
import com.expleague.sensearch.donkey.writers.PageWriter;
import com.expleague.sensearch.donkey.writers.TermWriter;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.Link;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

public class PlainIndexBuilder implements IndexBuilder {

  private static final int STATISTICS_BLOCK_SIZE = 1 << 10;
  private static final int PLAIN_PAGE_BLOCK_SIZE = 1 << 20;
  private static final int PLAIN_TERM_BLOCK_SIZE = 1 << 20;

  static final int BATCH_SIZE = 1_000;

  public static final String PAGE_ROOT = "page";
  public static final String TERM_ROOT = "term";
  public static final String LINK_ROOT = "link";

  public static final String TERM_STATISTICS_ROOT = "stats";

  public static final String EMBEDDING_ROOT = "embedding";
  public static final String URI_MAPPING_ROOT = "uriMapping";
  public static final String LSH_METRIC_ROOT = "lsh_metric";
  public static final String LSH_ROOT = "lsh";
  public static final String TEMP_EMBEDDING_ROOT = "temp_embedding";
  public static final String VECS_ROOT = "vecs";

  public static final Path RARE_INV_IDX_FILE = Paths.get("rare_iidx").resolve("iidx");

  public static final String SUGGEST_UNIGRAM_ROOT = "suggest/unigram_coeff";
  public static final String SUGGEST_MULTIGRAMS_ROOT = "suggest/multigram_freq_norm";

  public static final String INDEX_META_FILE = "index.meta";
  public static final int DEFAULT_VEC_SIZE = 300;

  private static final int NEIGHBORS_NUM = 50;
  private static final int NUM_OF_RANDOM_IDS = 100;

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

  private static final Options EMBEDDING_DB_OPTIONS =
      new Options()
          .createIfMissing(true)
          .errorIfExists(true)
          .compressionType(CompressionType.SNAPPY);

  private static final Options URI_DB_OPTIONS =
      new Options()
          .createIfMissing(true)
          .errorIfExists(true)
          .compressionType(CompressionType.SNAPPY);

  private static final String[] REQUIRED_WORDS = new String[]{"путин", "медведев", "александр"};

  private static final Logger LOG = Logger.getLogger(PlainIndexBuilder.class);
  private final Crawler crawler;
  private final Lemmer lemmer;
  private final Tokenizer tokenizer = new TokenizerImpl();
  private final Path indexRoot;
  private final Path embeddingVectorsPath;

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
    LOG.info(String.format("Index build in [%.3f] seconds", (System.nanoTime() - startTime) / 1e9));
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
            new SuggestInformationBuilder(index, config.getIndexRoot(), suggestUnigramDb,
                suggestMultigramDb);

        suggestBuilder.build();

        LOG.info(String
            .format("Suggest index was built in %.1f sec", (System.nanoTime() - start) / 1e9));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void buildRawIndex() throws IOException {
    Files.createDirectories(indexRoot.resolve(TERM_ROOT));
    Files.createDirectories(indexRoot.resolve(PAGE_ROOT));
    Files.createDirectories(indexRoot.resolve(LINK_ROOT));

    try (
        TermWriter termWriter = new TermWriter(indexRoot.resolve(TERM_ROOT));
        Dictionary dictionary = new Dictionary(termWriter);
        TokenParser parser = new TokenParser(dictionary, lemmer);
        LinkWriter linkWriter = new LinkWriter(indexRoot.resolve(LINK_ROOT));
        PageWriter pageWriter = new PageWriter(indexRoot.resolve(PAGE_ROOT), parser, linkWriter);
        ) {
      crawler.makeStream().forEach(pageWriter::write);
    }
  }

  private void buildLinks() {
    TLongObjectMap<Page.Builder> pagesCache = new TLongObjectHashMap<>();
    Reader<Link> linkReader = new LinkReader(indexRoot.resolve(LINK_ROOT));
    LevelDbBasedIndex<Page> pageIndex = new PageIndex(indexRoot.resolve(PAGE_ROOT));
    Link link;

    while ((link = linkReader.read()) != null) {
      long targetId = link.getTargetPageId();
      if (!pagesCache.containsKey(targetId)) {
        Page.Builder page = Page.newBuilder(pageIndex.getValue(targetId));
        pagesCache.put(targetId, page);
      }
      pagesCache.get(targetId).addIncomingLinks(link);

      if (pagesCache.size() >= 10_000) {
        pagesCache.forEachEntry((k, v) -> {
          pageIndex.put(k, v.build());
          return true;
        });
        pagesCache = new TLongObjectHashMap<>();
      }
    }
  }

  private void buildIndexInternal(Embedding<CharSeq> jmllEmbedding) throws IOException {
    LOG.info("Creating database files...");
    Files.createDirectories(indexRoot.resolve(PAGE_ROOT));
    Files.createDirectories(indexRoot.resolve(TERM_STATISTICS_ROOT));
    Files.createDirectories(indexRoot.resolve(EMBEDDING_ROOT));
    Files.createDirectories(indexRoot.resolve(URI_MAPPING_ROOT));
    Files.createDirectories(indexRoot.resolve(TERM_ROOT));

    final TLongSet knownPageIds = new TLongHashSet();
    try (final PlainPageBuilder plainPageBuilder =
        new PlainPageBuilder(
            JniDBFactory.factory.open(indexRoot.resolve(PAGE_ROOT).toFile(), PAGE_DB_OPTIONS),
            indexRoot.resolve(PAGE_ROOT).resolve("TMP"));
        final TermWriter termWriter = new TermWriter(indexRoot.resolve(TERM_ROOT));
        final StatisticsBuilder statisticsBuilder =
            new StatisticsBuilder(indexRoot.resolve(TERM_STATISTICS_ROOT));
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
                tokenizer);
        final UriMappingBuilder uriMappingBuilder =
            new UriMappingBuilder(
                JniDBFactory.factory.open(
                    indexRoot.resolve(URI_MAPPING_ROOT).toFile(), URI_DB_OPTIONS))) {

      EmbeddingImpl<CharSeq> jmllEmbedding1 = (EmbeddingImpl<CharSeq>) jmllEmbedding;
      for (int i = 0; i < jmllEmbedding1.vocabSize(); i++) {
        ParsedTerm parsedTerm = termParser.parseTerm(jmllEmbedding1.getObj(i));
        embeddingBuilder.addTerm(parsedTerm.wordId(), jmllEmbedding1.apply(parsedTerm.word()));
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
                                      ParsedTerm parsedTerm = termParser.parseTerm(word);
                                      termWriter.write(parser.addToken(word));
                                      // TODO: uncomment it
                                      /*
                                      if (jmllEmbedding.apply(CharSeq.compact(word)) == null) {
                                        rareTermsInvIdx
                                            .putIfAbsent(termLemmaId.id, new TLongArrayList());
                                        rareTermsInvIdx.get(termLemmaId.id).add(pageId);
                                      }
                                      */
                                      statisticsBuilder.enrich(parsedTerm);
                                      indexMetaBuilder.addTerm(
                                          parsedTerm.wordId(),
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
        FileUtils.deleteDirectory(indexRoot.resolve(PAGE_ROOT).toFile());
        FileUtils.deleteDirectory(indexRoot.resolve(TERM_STATISTICS_ROOT).toFile());
        FileUtils.deleteDirectory(indexRoot.resolve(EMBEDDING_ROOT).toFile());
        FileUtils.deleteDirectory(indexRoot.resolve("suggest").toFile());
        FileUtils.deleteDirectory(indexRoot.resolve(URI_MAPPING_ROOT).toFile());
        FileUtils.deleteDirectory(indexRoot.resolve(TERM_ROOT).toFile());

        throw new IOException(e);
      }
    }
    LOG.info("Index built!");
  }
}
