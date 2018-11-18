package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.Crawler;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public class PlainIndexBuilder implements IndexBuilder {

  private static final String STATISTICS_ROOT = "stats";
  private static final String PLAIN_ROOT = "plain";
  private static final String EMBEDDING_ROOT = "embedding";

  private static final int VEC_SIZE = 50;

  private static final Logger LOG = Logger.getLogger(PlainIndexBuilder.class.getName());

  public PlainIndexBuilder() {
  }

  static int firstWordId(long bigramId) {
    return (int) (bigramId >>> 32);
  }

  static int secondWordId(long bigramId) {
    return (int) bigramId;
  }

  @Override
  public void buildIndex(Crawler crawler, Config config) throws IOException {
    final TLongObjectMap<Vec> gloveVectors = new TLongObjectHashMap<>();
    final TObjectIntMap<String> idMappings = new TObjectIntHashMap<>();
    readGloveVectors(Paths.get(config.getEmbeddingVectors()), idMappings, gloveVectors);

    final Path indexRoot = config.getTemporaryIndex();
    final PlainPageBuilder plainPageBuilder = new PlainPageBuilder(indexRoot.resolve(PLAIN_ROOT));
    final StatisticsBuilder statisticsBuilder = new StatisticsBuilder(indexRoot.resolve(STATISTICS_ROOT));
    final EmbeddingBuilder embeddingBuilder = new EmbeddingBuilder();

    final long[] pagesAndTokensCounts = new long[]{0, 0};

    try {
      crawler.makeStream().forEach(
          doc -> {
            int pageId = plainPageBuilder.add(doc);

            embeddingBuilder.add(
                toLongPageId(pageId),
                toVector(
                    toWordIds(
                        Tokenizer.tokenize(doc.getTitle().toLowerCase()),
                        idMappings
                    ),
                    gloveVectors
                )
            );

            int[] tokens = toWordIds(
                Tokenizer.tokenize((doc.getTitle() + " " + doc.getTitle()).toLowerCase()),
                idMappings
            );

            final TIntIntMap termFrequencyMap = new TIntIntHashMap();
            final TIntObjectMap<TIntIntMap> bigramFrequencyMap = new TIntObjectHashMap<>();
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

      plainPageBuilder.build();
      statisticsBuilder.build();
      embeddingBuilder.build(indexRoot.resolve(EMBEDDING_ROOT));
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private long toBigramId(int word1, int word2) {
    return (long) word1 << 32 + word2;
  }

  private long toLongPageId(int pageId) {
    return -pageId;
  }

  private int[] toWordIds(String[] words, TObjectIntMap<String> mappings) {
    TIntList wordIdsList = new TIntLinkedList();
    for (int i = 0; i < words.length; ++i) {
      if (mappings.containsKey(words[i])) {
        wordIdsList.add(mappings.get(words[i]));
      } else {
        LOG.warning(String.format("For word '%s' was not found vector representation!", words[i]));
      }
    }

    return wordIdsList.toArray();
  }

  private void enrichFrequencies(int[] tokens, TIntIntMap termFrequencyMap,
      TIntObjectMap<TIntIntMap> bigramFrequencyMap) {
    if (tokens.length < 1) {
      return;
    }
    termFrequencyMap.put(tokens[0], 1);
    for (int i = 1; i < tokens.length; ++i) {
      termFrequencyMap.adjustOrPutValue(tokens[i], 1, 1);

      bigramFrequencyMap.putIfAbsent(tokens[i - 1], new TIntIntHashMap());
      bigramFrequencyMap.get(tokens[i - 1]).adjustOrPutValue(tokens[i], 1, 1);
    }
  }

  private Vec toVector(int[] tokens, TLongObjectMap<Vec> vectors) {
    ArrayVec mean = new ArrayVec(new double[VEC_SIZE]);
    for (int i : tokens) {
      mean.add((ArrayVec) vectors.get(i));
    }
    mean.scale(1.0 / tokens.length);
    return mean;
  }

  private void readGloveVectors(Path glovePath, TObjectIntMap<String> idMappings,
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
                  idMappings.put(word, idMappings.size());
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
