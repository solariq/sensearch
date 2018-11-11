package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.seq.CharSeq;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.IndexBuilder;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
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

      statisticsBuilder.flushStatics(indexRoot);
    } catch (Exception e) {
      throw new IOException(e);
    }

  }
}
