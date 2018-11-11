package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.nio.file.Path;

class StatisticsBuilder {

  private final TObjectIntMap<String> wordToIdMappings = new TObjectIntHashMap<>();

  private final TLongLongMap wordFrequencyMap = new TLongLongHashMap();
  private final TLongIntMap documentFrequencyMap = new TLongIntHashMap();
  // docsAndWordsCounts[0] -- current documents count
  // docsAndWordsCounts[1] -- overall tokens count in observed documents
  private final double[] docsAndWordsCounts = new double[2];

  private final Config config;
  StatisticsBuilder(Config config) {
    this.config = config;
  }

  void enrichStatistics(CrawlerDocument crawlerDocument) {

  }

  void flushStatics() {
    // TODO: save statistics to disk
  }

  TObjectIntMap<String> wordToIntMappings() {
    return wordToIdMappings;
  }
}
