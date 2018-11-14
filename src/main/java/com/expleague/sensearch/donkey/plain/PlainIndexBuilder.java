package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.Crawler;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

public class PlainIndexBuilder implements IndexBuilder {

  private static final String STATISTICS_ROOT = "stats";
  private static final String PLAIN_ROOT = "plain";
  private static final String EMBEDDING_ROOT = "embedding";

  public PlainIndexBuilder(Config config) {
  }

  @Override
  public void buildIndex(Crawler crawler, Config config) throws IOException {
    final Path indexRoot = config.getTemporaryIndex();
    final PlainPageBuilder plainPageBuilder = new PlainPageBuilder(indexRoot.resolve(""));
    final StatisticsBuilder statisticsBuilder = new StatisticsBuilder();
    final EmbeddingBuilder embeddingBuilder = new EmbeddingBuilder(config);
    try {
      crawler.makeStream().forEach(
          doc -> {
            plainPageBuilder.createAndFlushNewPage(doc);
            statisticsBuilder.enrichStatistics(doc);
            embeddingBuilder.addNewPageVector(doc, plainPageBuilder.currentDocumentId());
          }
      );

      statisticsBuilder.flushStatics(indexRoot.);
      embeddingBuilder.flushVectors();
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private TObjectIntMap<String> wordToIdMappingFromGlove(Path glovePath) {
    TObjectIntMap<String> wordIdMappings = new TObjectIntHashMap<>();
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
                final Vec vec = new ArrayVec(doubles);
                synchronized (wordIdMappings) {
                  wordIdMappings.put(word, wordIdMappings.size() + 1);
                }
              }
          );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
