package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.Crawler;
import gnu.trove.map.TObjectIntMap;
import java.io.IOException;
import java.nio.file.Path;

public class PlainIndexBuilder implements IndexBuilder {

  public PlainIndexBuilder(Config config) {
  }

  @Override
  public void buildIndex(Crawler crawler, Config config) throws IOException {
    final Path indexRoot = config.getTemporaryIndex();
    final PlainPageBuilder plainPageBuilder = new PlainPageBuilder(indexRoot);
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

      TObjectIntMap<String> wordMappings =
          embeddingBuilder.replaceWordsWithIds(statisticsBuilder.wordToIntMappings());
      statisticsBuilder.flushStatics(indexRoot);
      embeddingBuilder.
    } catch (Exception e) {
      throw new IOException(e);
    }

  }
}
