package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta.IdMapping;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta.LemmaIdMapping;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta.UriPageMapping;
import com.google.common.annotations.VisibleForTesting;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.apache.log4j.Logger;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

public class PlainIndexBuilder implements IndexBuilder {
  public static final int STATISTICS_BLOCK_SIZE = 1 << 10;
  public static final int PLAIN_PAGE_BLOCK_SIZE = 1 << 20;

  public static final String TERM_STATISTICS_ROOT = "stats";
  public static final String PLAIN_ROOT = "plain";
  public static final String EMBEDDING_ROOT = "embedding";

  public static final String INDEX_META_FILE = "index.meta";
  public static final int DEFAULT_VEC_SIZE = 130;

  // DB configurations
  private static final long DEFAULT_CACHE_SIZE = 1 << 10; // 1 KB
  private static final Options STATS_DB_OPTIONS =
      new Options()
          .cacheSize(DEFAULT_CACHE_SIZE)
          .blockSize(STATISTICS_BLOCK_SIZE) // 1 MB
          .createIfMissing(true)
          .errorIfExists(true)
          .compressionType(CompressionType.SNAPPY);
  private static final Options PLAIN_DB_OPTIONS =
      new Options()
          .cacheSize(DEFAULT_CACHE_SIZE)
          .blockRestartInterval(PLAIN_PAGE_BLOCK_SIZE)
          .createIfMissing(true)
          .errorIfExists(true)
          .compressionType(CompressionType.SNAPPY);

  private static final Logger LOG = Logger.getLogger(PlainIndexBuilder.class.getName());

  public PlainIndexBuilder() {
  }


  // TODO refactor this
  // Yes, this is a dirty copy-paste
  @VisibleForTesting
  static Iterable<UriPageMapping> toProtobufIterableUri(TObjectLongMap<String> mappings) {
    final List<UriPageMapping> protobufMappings = new LinkedList<>();
    final UriPageMapping.Builder uriMappingsBuilder = UriPageMapping.newBuilder();
    mappings.forEachEntry(
        (w, id) -> {
          protobufMappings.add(uriMappingsBuilder.setPageId(id).setUri(w).build());
          return true;
        });

    return protobufMappings;
  }

  // TODO refactor this
  // Another copy-paste
  @VisibleForTesting
  static Iterable<LemmaIdMapping> toProtobufIterableLemmaId(TLongLongMap mappings) {
    final List<LemmaIdMapping> protobufMappings = new LinkedList<>();
    final LemmaIdMapping.Builder idMappingsBuilder = LemmaIdMapping.newBuilder();
    mappings.forEachEntry(
        (w, lemma) -> {
          protobufMappings.add(idMappingsBuilder.setWordId(w).setLemmaId(lemma).build());
          return true;
        });

    return protobufMappings;
  }

  @VisibleForTesting
  static Iterable<IdMapping> toProtobufIterable(TObjectLongMap<String> mappings) {
    final List<IdMapping> protobufMappings = new LinkedList<>();
    final IdMapping.Builder idMappingsBuilder = IdMapping.newBuilder();
    mappings.forEachEntry(
        (w, id) -> {
          protobufMappings.add(idMappingsBuilder.setId(id).setWord(w).build());
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
    return words.map(Object::toString).mapToLong(word -> {
      if (!mappings.containsKey(word)) {
        LOG.warn(String.format("For the word '%s' was not found any vector representation!", word));
        mappings.put(word, mappings.size() + 1);
      }
      return mappings.get(word);
    }).toArray();
  }

  @VisibleForTesting
  static void enrichFrequencies(
      long[] tokens, TLongIntMap termFrequencyMap, TLongObjectMap<TLongIntMap> bigramFrequencyMap) {
    if (tokens.length < 1) {
      return;
    }
    termFrequencyMap.put(tokens[0], 1);
    for (int i = 1; i < tokens.length; ++i) {
      termFrequencyMap.adjustOrPutValue(tokens[i], 1, 1);

      bigramFrequencyMap.putIfAbsent(tokens[i - 1], new TLongIntHashMap());
      bigramFrequencyMap.get(tokens[i - 1]).adjustOrPutValue(tokens[i], 1, 1);
    }
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

  @VisibleForTesting
  static void readGloveVectors(
      Path glovePath, TObjectLongMap<String> idMappings, TLongObjectMap<Vec> vectors) {
    try (Reader input =
        new InputStreamReader(
            new GZIPInputStream(new FileInputStream(glovePath.toFile())), StandardCharsets.UTF_8)) {
      CharSeqTools.lines(input)
          .parallel()
          .forEach(
              line -> {
                CharSequence[] parts = CharSeqTools.split(line, ' ');
                final String word = parts[0].toString().toLowerCase();
                double[] doubles =
                    Arrays.stream(parts, 1, parts.length)
                        .mapToDouble(CharSeqTools::parseDouble)
                        .toArray();
                synchronized (idMappings) {
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

  @Override
  public void buildIndex(Crawler crawler, Config config, Lemmer lemmer) throws IOException {
    final Tokenizer tokenizer = new TokenizerImpl();
    final TLongObjectMap<Vec> gloveVectors = new TLongObjectHashMap<>();
    final TObjectLongMap<String> idMappings = new TObjectLongHashMap<>();
    final TObjectLongMap<String> uriPageIdMapping = new TObjectLongHashMap<>();

    LOG.info("Reading vectors...");
    readGloveVectors(Paths.get(config.getEmbeddingVectors()), idMappings, gloveVectors);

    LOG.info("Parsing lemmas...");
    TLongLongMap lemmaMappings = enrichWordsWithLemmas(idMappings, lemmer);

    LOG.info("Creating database files...");
    final Path indexRoot = config.getTemporaryIndex();
    // ensure all roots
    Files.createDirectories(indexRoot.resolve(PLAIN_ROOT));
    final DB plainDb =
        JniDBFactory.factory.open(indexRoot.resolve(PLAIN_ROOT).toFile(), PLAIN_DB_OPTIONS);
    final PlainPageBuilder plainPageBuilder = new PlainPageBuilder(plainDb);

    Files.createDirectories(indexRoot.resolve(TERM_STATISTICS_ROOT));
    final DB statisticsDb =
        JniDBFactory.factory.open(
            indexRoot.resolve(TERM_STATISTICS_ROOT).toFile(), STATS_DB_OPTIONS);
    final StatisticsBuilder statisticsBuilder = new StatisticsBuilder(statisticsDb);

    Files.createDirectories(indexRoot.resolve(EMBEDDING_ROOT));
    final EmbeddingBuilder embeddingBuilder =
        new EmbeddingBuilder(indexRoot.resolve(EMBEDDING_ROOT));

    final long[] pagesAndTokensCounts = new long[]{0, 0};

    final TLongIntMap termFrequencyMap = new TLongIntHashMap();
    final TLongObjectMap<TLongIntMap> bigramFrequencyMap = new TLongObjectHashMap<>();

    // saving page-wise data
    LOG.info("Storing page-wise data...");
    try {
      crawler
          .makeStream()
          .forEach(
              doc -> {
                long pageId = plainPageBuilder.add(doc);

                long[] titleIds = toIds(tokenizer.parseTextToWords(doc.title().toLowerCase()),
                    idMappings);

                embeddingBuilder.add(pageId, toVector(titleIds, gloveVectors));

                long[] tokens = toIds(
                    tokenizer.parseTextToWords((doc.title() + " " + doc.content()).toLowerCase()),
                    idMappings);

                termFrequencyMap.clear();
                bigramFrequencyMap.clear();
                enrichFrequencies(tokens, termFrequencyMap, bigramFrequencyMap);
                statisticsBuilder.enrich(termFrequencyMap, bigramFrequencyMap);

                ++pagesAndTokensCounts[0];
                pagesAndTokensCounts[1] += tokens.length;

                uriPageIdMapping.put(doc.uri().toString(), pageId);
              });

      LOG.info("Building embedding...");
      embeddingBuilder.addAll(gloveVectors);
      embeddingBuilder.build();

      plainPageBuilder.build();
      statisticsBuilder.build();

      LOG.info("Storing index meta...");
      // saving index-wise data
      IndexUnits.IndexMeta.newBuilder()
          .setVersion(PlainIndex.VERSION)
          .setAveragePageSize((double) pagesAndTokensCounts[1] / pagesAndTokensCounts[0])
          .setVocabularySize(idMappings.size())
          .setPagesCount((int) pagesAndTokensCounts[0])
          .addAllIdMappings(toProtobufIterable(idMappings))
          .addAllLemmaIdMappings(toProtobufIterableLemmaId(lemmaMappings))
          .addAllUriPageMappings(toProtobufIterableUri(uriPageIdMapping))
          .build()
          .writeTo(Files.newOutputStream(indexRoot.resolve(INDEX_META_FILE)));

    } catch (Exception e) {
      throw new IOException(e);
    }
    LOG.info("Index built!");
  }

  private TLongLongMap enrichWordsWithLemmas(TObjectLongMap<String> idMapping, Lemmer lemmer) {
    MyStem stem = lemmer.myStem;
    TLongLongMap mapping = new TLongLongHashMap();
    TObjectLongMap<String> newIds = new TObjectLongHashMap<>();

    idMapping.forEachKey(word -> {
      final List<WordInfo> parse = stem.parse(word);
      final LemmaInfo lemma = parse.size() > 0 ? parse.get(0).lemma() : null;
      ;

      //noinspection EqualsBetweenInconvertibleTypes
      if (lemma == null || lemma.lemma().equals(word)) {
        mapping.put(idMapping.get(word), -1);
        return true;
      }

      if (!idMapping.containsKey(lemma.lemma()) && !newIds.containsKey(lemma.lemma().toString())) {
        newIds.put(lemma.lemma().toString(), idMapping.size() + newIds.size());
      }

      long lemmaId = idMapping.get(lemma);
      mapping.put(idMapping.get(word), lemmaId);
      if (!mapping.containsKey(lemmaId)) {
        mapping.put(lemmaId, -1);
      }

      return true;
    });

    idMapping.putAll(newIds);
    return mapping;
  }

}
