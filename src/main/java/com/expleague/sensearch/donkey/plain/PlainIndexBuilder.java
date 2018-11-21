package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class PlainIndexBuilder implements IndexBuilder {

  public static final String TERM_STATISTICS_ROOT = "stats";
  public static final String PLAIN_ROOT = "plain";
  public static final String EMBEDDING_ROOT = "embedding";

  public static final String WORD_ID_MAPPINGS = "word2id.map.gz";
  public static final String INDEX_STATISTICS = "index.stat";

  private static final int VEC_SIZE = 50;

  private static final Logger LOG = Logger.getLogger(PlainIndexBuilder.class.getName());

  public PlainIndexBuilder() {
  }

  @Override
  public void buildIndex(Crawler crawler, Config config) throws IOException {
    final TLongObjectMap<Vec> gloveVectors = new TLongObjectHashMap<>();
    final TObjectLongMap<String> idMappings = new TObjectLongHashMap<>();
    readGloveVectors(Paths.get(config.getEmbeddingVectors()), idMappings, gloveVectors);

    final Path indexRoot = config.getTemporaryIndex();
    final PlainPageBuilder plainPageBuilder = new PlainPageBuilder(indexRoot.resolve(PLAIN_ROOT));
    final StatisticsBuilder statisticsBuilder = new StatisticsBuilder(
        indexRoot.resolve(TERM_STATISTICS_ROOT));
    final EmbeddingBuilder embeddingBuilder = new EmbeddingBuilder(indexRoot.resolve(EMBEDDING_ROOT));

    final long[] pagesAndTokensCounts = new long[]{0, 0};

    final TLongIntMap termFrequencyMap = new TLongIntHashMap();
    final TLongObjectMap<TLongIntMap> bigramFrequencyMap = new TLongObjectHashMap<>();

    // saving page-wise data
    try {
      crawler.makeStream().forEach(
          doc -> {
            long pageId = plainPageBuilder.add(doc);

            embeddingBuilder.add(
                pageId,
                toVector(
                    toWordIds(
                        Tokenizer.tokenize(doc.getTitle().toLowerCase()),
                        idMappings
                    ),
                    gloveVectors
                )
            );

            long[] tokens = toWordIds(
                Tokenizer.tokenize((doc.getTitle() + " " + doc.getTitle()).toLowerCase()),
                idMappings
            );

            termFrequencyMap.clear();
            bigramFrequencyMap.clear();
            enrichFrequencies(tokens, termFrequencyMap, bigramFrequencyMap);
            statisticsBuilder.enrich(
                termFrequencyMap,
                bigramFrequencyMap
            );

            ++pagesAndTokensCounts[0];
            pagesAndTokensCounts[1] += tokens.length;
          }
      );
      embeddingBuilder.addAll(gloveVectors);
      embeddingBuilder.build();

      plainPageBuilder.build();
      statisticsBuilder.build();

      // saving index-wise data
      IndexUnits.IndexStatistics
          .newBuilder()
          .setAveragePageSize((double) pagesAndTokensCounts[1] / pagesAndTokensCounts[0])
          .setVocabularySize(idMappings.size())
          .setPagesCount((int) pagesAndTokensCounts[0])
          .build()
          .writeTo(Files.newOutputStream(indexRoot.resolve(INDEX_STATISTICS)));

      flushWordToIdMap(indexRoot.resolve(WORD_ID_MAPPINGS), idMappings);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private void flushWordToIdMap(Path pathToMap, TObjectLongMap<String> idMappings)
      throws IOException {
    StringBuilder idMappingsSb = new StringBuilder();
    idMappings.forEachEntry(
        (s, id) -> {
          idMappingsSb
              .append(s)
              .append("\t")
              .append(id)
              .append("\n");
          return true;
        }
    );

    try (
        BufferedWriter mapWriter = new BufferedWriter(
            new OutputStreamWriter(
                new GZIPOutputStream(Files.newOutputStream(pathToMap))
            )
        )
    ) {
      mapWriter.write(idMappingsSb.toString().trim());
    }
  }

  private long toLongPageId(int pageId) {
    return -pageId;
  }

  /**
   * Converts an array of words to an array of integer ids. Also if a word was not found in
   * mappings the method adds a new entry to mapping for such word
   *
   * @param words array of words to be converted to ids
   * @param mappings all known word to int mappings
   * @return array of word ids in the same order as given words
   */
  private long[] toWordIds(String[] words, TObjectLongMap<String> mappings) {
    long[] wordIds = new long[words.length];
    for (int i = 0; i < words.length; ++i) {
      if (!mappings.containsKey(words[i])) {
        LOG.warning(String.format("For the word '%s' was not found any vector representation!",
            words[i])
        );
        mappings.put(words[i], mappings.size());
      }
      wordIds[i] = mappings.get(words[i]);
    }

    return wordIds;
  }

  private void enrichFrequencies(long[] tokens, TLongIntMap termFrequencyMap,
      TLongObjectMap<TLongIntMap> bigramFrequencyMap) {
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

  private Vec toVector(long[] tokens, TLongObjectMap<Vec> vectors) {
    ArrayVec mean = new ArrayVec(new double[VEC_SIZE]);
    for (long i : tokens) {
      mean.add((ArrayVec) vectors.get(i));
    }
    mean.scale(1.0 / tokens.length);
    return mean;
  }

  private void readGloveVectors(Path glovePath, TObjectLongMap<String> idMappings,
      TLongObjectMap<Vec> vectors) {
    try (Reader input =
        new InputStreamReader(
            new GZIPInputStream(
                new FileInputStream(glovePath.toFile())),
            StandardCharsets.UTF_8
        )
    ) {
      CharSeqTools.lines(input)
          .parallel()
          .forEach(line -> {
                CharSequence[] parts = CharSeqTools.split(line, ' ');
                final String word = parts[0].toString();
                double[] doubles = Arrays.stream(parts, 1, parts.length)
                    .mapToDouble(CharSeqTools::parseDouble)
                    .toArray();
                synchronized (idMappings) {
                  idMappings.put(word, idMappings.size() + 1);
                }
                synchronized (vectors) {
                  vectors.put(idMappings.get(word), new ArrayVec(doubles));
                }
              }
          );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
