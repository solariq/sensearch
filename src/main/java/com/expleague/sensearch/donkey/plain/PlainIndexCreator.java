package com.expleague.sensearch.donkey.plain;

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
import com.expleague.sensearch.donkey.IndexCreator;
import com.expleague.sensearch.donkey.statistics.IndexStatisticsBuilder;
import com.expleague.sensearch.donkey.statistics.PageStatisticsBuilder;
import com.expleague.sensearch.donkey.statistics.PageStatisticsBuilderFactory;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.embedding.EmbeddedPage;
import com.expleague.sensearch.donkey.embedding.PageEmbedder;
import com.expleague.sensearch.donkey.plain.IndexMetaBuilder.TermSegment;
import com.expleague.sensearch.donkey.randomaccess.ProtoPageIndex;
import com.expleague.sensearch.donkey.randomaccess.ProtoTermIndex;
import com.expleague.sensearch.donkey.randomaccess.ProtoTermStatisticsIndex;
import com.expleague.sensearch.donkey.randomaccess.RandomAccess;
import com.expleague.sensearch.donkey.readers.LevelDbLinkReader;
import com.expleague.sensearch.donkey.readers.Reader;
import com.expleague.sensearch.donkey.readers.SequentialLinkReader;
import com.expleague.sensearch.donkey.term.Dictionary;
import com.expleague.sensearch.donkey.term.ParsedTerm;
import com.expleague.sensearch.donkey.term.TokenParser;
import com.expleague.sensearch.donkey.utils.BrandNewIdGenerator;
import com.expleague.sensearch.donkey.utils.ExternalSorter;
import com.expleague.sensearch.donkey.utils.SerializedTextHelperFactory;
import com.expleague.sensearch.donkey.writers.EmbeddingWriter;
import com.expleague.sensearch.donkey.writers.LevelDbLinkWriter;
import com.expleague.sensearch.donkey.writers.PageWriter;
import com.expleague.sensearch.donkey.writers.TermWriter;
import com.expleague.sensearch.donkey.writers.sequential.SequentialLinkWriter;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.Link;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term;
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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

public class PlainIndexCreator implements IndexCreator {

  private static final int STATISTICS_BLOCK_SIZE = 1 << 10;
  private static final int PLAIN_PAGE_BLOCK_SIZE = 1 << 20;
  private static final int PLAIN_TERM_BLOCK_SIZE = 1 << 20;

  static final int BATCH_SIZE = 1_000;

  public static final int INDEX_VERSION = 20;

  public static final String PAGE_ROOT = "page";
  public static final String TERM_ROOT = "term";
  public static final String RAW_LINK_ROOT = "RawLinks";
  public static final String SORTED_LINKS = "SortedLinks";

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

  private static final Logger LOG = Logger.getLogger(PlainIndexCreator.class);
  private final Crawler crawler;
  private final Lemmer lemmer;
  private final Tokenizer tokenizer = new TokenizerImpl();
  private final Path indexRoot;
  private final Path embeddingVectorsPath;

  @Inject
  public PlainIndexCreator(
      Crawler crawler,
      @IndexRoot Path indexRoot,
      @EmbeddingVectorsPath Path embeddingVectorsPath,
      Lemmer lemmer) {
    this.crawler = crawler;
    this.indexRoot = indexRoot;
    this.embeddingVectorsPath = embeddingVectorsPath;

    this.lemmer = lemmer;
  }

  @Override
  public void createWordEmbedding() {
    LOG.info("training JMLL embedding...");
    EmbeddingImpl<CharSeq> jmllEmbedding;
    try {
      jmllEmbedding =
          (EmbeddingImpl<CharSeq>)
              new JmllEmbeddingBuilder(DEFAULT_VEC_SIZE, indexRoot.resolve(TEMP_EMBEDDING_ROOT))
                  .build(crawler.makeStream());
      jmllEmbedding.write(new FileWriter(embeddingVectorsPath.toFile()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void createSuggest() {

    try {
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
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void createPagesAndTerms() {
    long startTime;
    LOG.info("Creating page and term databases...");
    try {
      Files.createDirectories(indexRoot.resolve(TERM_ROOT));
      Files.createDirectories(indexRoot.resolve(PAGE_ROOT));
      Files.createDirectories(indexRoot.resolve(RAW_LINK_ROOT));

      try (
          PageWriter pageWriter = new PageWriter(
              indexRoot.resolve(PAGE_ROOT),
              new TokenParser(new Dictionary(new TermWriter(indexRoot.resolve(TERM_ROOT))), lemmer,
                  new TokenizerImpl()),
              new LevelDbLinkWriter(indexRoot.resolve(RAW_LINK_ROOT))
          )
      ) {
        startTime = System.nanoTime();
        crawler.makeStream().forEach(pageWriter::write);
      }
    } catch (Exception e) {
      cleanup(indexRoot.resolve(TERM_ROOT));
      cleanup(indexRoot.resolve(PAGE_ROOT));
      cleanup(indexRoot.resolve(RAW_LINK_ROOT));
      throw new RuntimeException(e);
    }
    LOG.info(String.format("Page and term databases created in %.2f seconds",
        (System.nanoTime() - startTime) / 1e9));
  }

  @Override
  public void createLinks() {
    sortLinksByTargetId();
    resolveIncomingLinks();
  }

  private void sortLinksByTargetId() {
    ExternalSorter.sort(
        new LevelDbLinkReader(indexRoot.resolve(RAW_LINK_ROOT)),
        indexRoot.resolve(SORTED_LINKS),
        Comparator.comparingLong(Link::getTargetPageId),
        SequentialLinkWriter::new,
        SequentialLinkReader::new
    );
    cleanup(indexRoot.resolve(RAW_LINK_ROOT));
  }

  private void resolveIncomingLinks() {
    try (
        Reader<Link> sequentialReader = new SequentialLinkReader(indexRoot.resolve(SORTED_LINKS));
        RandomAccess<Long, Page> pageIndex = new ProtoPageIndex(indexRoot.resolve(PAGE_ROOT))
    ) {
      Link link = sequentialReader.read();
      if (link == null) {
        throw new RuntimeException("No sorted links was found!");
      }

      long currentTargetId = link.getTargetPageId();
      Page.Builder currentTargetPage = Page.newBuilder(pageIndex.value(currentTargetId));
      currentTargetPage.addIncomingLinks(link);
      while ((link = sequentialReader.read()) != null) {
        if (link.getTargetPageId() != currentTargetId) {
          pageIndex.put(currentTargetId, currentTargetPage.build());
          currentTargetId = link.getTargetPageId();
          currentTargetPage = Page.newBuilder(pageIndex.value(currentTargetId));
        }
        currentTargetPage.addIncomingLinks(link);
      }
      FileUtils.forceDelete(indexRoot.resolve(SORTED_LINKS).toFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void createStats() {
    LOG.info("Creating stats...");
    final long startTime = System.nanoTime();

    Path statsPath = indexRoot.resolve(TERM_STATISTICS_ROOT);
    try {
      try (
          RandomAccess<Long, Page> pageIndex = new ProtoPageIndex(indexRoot.resolve(PAGE_ROOT));
          RandomAccess<Integer, Term> termIndex = new ProtoTermIndex(indexRoot.resolve(TERM_ROOT));
      ) {
        PageStatisticsBuilderFactory builderFactory = new PageStatisticsBuilderFactory(
            new SerializedTextHelperFactory(termIndex));
        IndexStatisticsBuilder indexStatisticsBuilder = new IndexStatisticsBuilder();

        pageIndex.forEach(p -> {
          if (p.getRootId() != p.getPageId()) {
            return;
          }
          PageStatisticsBuilder pageStatisticsBuilder = builderFactory.builder(p.getPageId());
          fullDocument(p, pageIndex)
              .peek(pageStatisticsBuilder::addPage)
              .map(Page::getOutgoingLinksList)
              .flatMap(List::stream)
              .mapToLong(Link::getTargetPageId)
              .distinct()
              .mapToObj(pageIndex::value)
              .filter(Objects::nonNull)
              .forEach(pageStatisticsBuilder::addTarget);
          indexStatisticsBuilder.addPageStatistics(pageStatisticsBuilder.build());
        });
      }
    } catch (Exception e) {
      cleanup(statsPath);
      throw new RuntimeException(e);
    }

    LOG.info(
        String.format("Statistics created in %.2f seconds", (System.nanoTime() - startTime) / 1e9));
  }

  private static Stream<Page> fullDocument(Page rootSection, RandomAccess<Long, Page> pageIndex) {
    if (rootSection == null) {
      return Stream.empty();
    }

    return Stream.concat(
        Stream.of(rootSection),
        rootSection.getSubpagesIdsList()
            .stream()
            .flatMap(id -> fullDocument(pageIndex.value(id), pageIndex))
    );
  }

  private static void cleanup(Path path) {
    try {
      FileUtils.forceDelete(path.toFile());
    } catch (IOException e) {
      LOG.error(String.format("Cannot cleanup path: %s", path.toAbsolutePath().toString()), e);
    }
  }

  @Override
  public void createPageEmbedding() {
    LOG.info("Creating page embedding...");

    EmbeddingImpl<CharSeq> wordEmbedding;
    try {
      final long startTime = System.nanoTime();

      wordEmbedding = EmbeddingImpl.read(
          Files.newBufferedReader(embeddingVectorsPath, Charset.forName("UTF-8")),
          CharSeq.class);

      LOG.info(String
          .format("Word embedding read in %.2f seconds", (System.nanoTime() - startTime) / 1e9));
    } catch (IOException e) {
      LOG.error("Cannot read embedding at path " + embeddingVectorsPath.toString(), e);
      throw new RuntimeException(e);
    }

    final long startTime = System.nanoTime();
    try (
        ProtoTermIndex termIndex = new ProtoTermIndex(indexRoot.resolve(TERM_ROOT));
        ProtoTermStatisticsIndex statsIndex = new ProtoTermStatisticsIndex(
            indexRoot.resolve(TERM_STATISTICS_ROOT));
        ProtoPageIndex pageIndex = new ProtoPageIndex(indexRoot.resolve(PAGE_ROOT));
        DB vecDb = JniDBFactory.factory
            .open(indexRoot.resolve(EMBEDDING_ROOT).toFile(), EMBEDDING_DB_OPTIONS);
        EmbeddingWriter embeddingWriter = new EmbeddingWriter(vecDb)
    ) {
      PageEmbedder pageEmbedder = new PageEmbedder(
          statsIndex, termIndex, new SerializedTextHelperFactory(termIndex), wordEmbedding);

      pageIndex.forEach(page -> {
        EmbeddedPage embedded = pageEmbedder.embed(page);
        embeddingWriter.write(embedded);
      });

    } catch (IOException e) {
      e.printStackTrace();
    }
    LOG.info(String
        .format("Page embedding created in %.2f seconds", (System.nanoTime() - startTime) / 1e9));
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
//        ParsedTerm parsedTerm = termParser.parseTerm(jmllEmbedding1.getObj(i));
//        embeddingBuilder.addTerm(parsedTerm.wordId(), jmllEmbedding1.apply(parsedTerm.word()));
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
                                      ParsedTerm parsedTerm = null; //termParser.parseTerm(word);
//                                      termWriter.write(parser.addToken(word));
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
