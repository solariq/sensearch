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
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

public class PlainIndexBuilder implements IndexBuilder {

  private static final String STATISTICS_ROOT = "stats";
  private static final String PLAIN_ROOT = "plain";
  private static final String EMBEDDING_ROOT = "embedding";
  private static final int VEC_SIZE = 50;

  public PlainIndexBuilder() {
  }

  @Override
  public void buildIndex(Crawler crawler, Config config) throws IOException {
    final TIntObjectMap<Vec> gloveVectors = new TIntObjectHashMap<>();
    final TObjectIntMap<String> idMappings = new TObjectIntHashMap<>();
    readGloveVectors(Paths.get(config.getEmbeddingVectors()), idMappings, gloveVectors);

    final PlainPageBuilder plainPageBuilder = new PlainPageBuilder();
    final StatisticsBuilder statisticsBuilder = new StatisticsBuilder();
    final EmbeddingBuilder embeddingBuilder = new EmbeddingBuilder();

    try {
      crawler.makeStream().forEach(
          doc -> {
            int pageId = plainPageBuilder.add(doc);
            embeddingBuilder.add(
                pageId,
                idsToVector(
                    wordsToIds(
                        Tokenizer.tokenize(doc.getTitle().toLowerCase()),
                        idMappings
                    ),
                    gloveVectors
                )
            );

            statisticsBuilder.enrich(
                wordsToIds(
                    Tokenizer.tokenize(
                        (doc.getTitle() + " " + doc.getContent()).toLowerCase()
                    ),
                    idMappings
                )
            );
          }
      );
      Path indexRoot = config.getTemporaryIndex();
      plainPageBuilder.build(indexRoot.resolve(PLAIN_ROOT));
      embeddingBuilder.build(indexRoot.resolve(EMBEDDING_ROOT));
      statisticsBuilder.build(indexRoot.resolve(STATISTICS_ROOT));
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private int[] wordsToIds(String[] words, TObjectIntMap<String> mappings) {
    TIntList ids = new TIntLinkedList();
    for (int i = 0; i < words.length; ++i) {
      if (mappings.containsKey(words[i])) {
        ids.add(mappings.get(words[i]));
      }
    }

    return ids.toArray();
  }

  private Vec idsToVector(int[] tokens, TIntObjectMap<Vec> vectors) {
    ArrayVec mean = new ArrayVec(new double[VEC_SIZE]);
    for (int i : tokens) {
      mean.add((ArrayVec) vectors.get(i));
    }
    mean.scale(1.0 / tokens.length);
    return mean;
  }

  private void readGloveVectors(Path glovePath, TObjectIntMap<String> idMappings,
      TIntObjectMap<Vec> vectors) {
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
