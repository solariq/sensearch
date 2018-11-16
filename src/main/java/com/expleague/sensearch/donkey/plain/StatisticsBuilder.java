package com.expleague.sensearch.donkey.plain;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import java.nio.file.Path;

class StatisticsBuilder {

  static final String WORD_FREQUENCY_MAP_FILE = "";
  static final String DOCUMENT_FREQUENCY_MAP_FILE = "";
  static final String BIGRAMS_MAP_FILE = "";

  private static final int MOST_FREQUENT_BIGRAMS_COUNT = 10;

  private final TIntLongMap wordFrequencyMap = new TIntLongHashMap();
  private final TIntIntMap documentFrequencyMap = new TIntIntHashMap();
  private final TLongIntMap largeBigramsMap = new TLongIntHashMap();

  StatisticsBuilder() {
  }

  void enrich(TIntIntMap pageWiseTf, TLongIntMap pageWiseBigramTf) {
    pageWiseTf.forEachEntry(
        (tok, freq) -> {
          wordFrequencyMap.adjustOrPutValue(tok, freq, freq);
          documentFrequencyMap.adjustOrPutValue(tok, 1, 1);
          return true;
        }
    );

    pageWiseBigramTf.forEachEntry(
        (tok, freq) -> {
          largeBigramsMap.adjustOrPutValue(tok, freq, freq);
          return true;
        }
    );
  }

  void build(Path statsRoot) {
  }

}
