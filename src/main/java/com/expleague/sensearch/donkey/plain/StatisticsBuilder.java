package com.expleague.sensearch.donkey.plain;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import java.nio.file.Path;

class StatisticsBuilder {

  static final String WORD_FREQUENCY_MAP_FILE = "";
  static final String DOCUMENT_FREQUENCY_MAP_FILE = "";
  static final String BIGRAMS_MAP_FILE = "";
  static final String META_STATISTICS_FILE = "";

  private static final int MOST_FREQUENT_BIGRAMS_COUNT = 10;

  private final TIntLongMap wordFrequencyMap = new TIntLongHashMap();
  private final TIntIntMap documentFrequencyMap = new TIntIntHashMap();
  private final TLongIntMap largeBigramsMap = new TLongIntHashMap();
  // docsAndWordsCounts[0] -- current documents count
  // docsAndWordsCounts[1] -- overall tokens count in observed documents
  private final long[] docsAndWordsCounts = new long[2];

  StatisticsBuilder() {
  }

  void enrich(TIntIntMap pageWiseTf, TLongIntMap pageWiseBigramTf, int pageSize) {
    ++docsAndWordsCounts[0];
    docsAndWordsCounts[1] += pageSize;
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

  private void trimBigrams(TIntObjectMap<TIntIntMap> largeBigramsMap) {
//    final NavigableMap<Integer, TIntLinkedList> freqHeap = new TreeMap<>(Comparator.reverseOrder());
//    int[] currentMapSize = new int[]{0};
//    largeBigramsMap.forEachEntry(
//        (tokId, neighMap) -> {
//          neighMap.forEachEntry(
//              (neighId, neighFreq) -> {
//                freqHeap.putIfAbsent(neighFreq, new TIntLinkedList());
//                if (currentMapSize[0] < MOST_FREQUENT_BIGRAMS_COUNT) {
//                  ++currentMapSize[0];
//                } else if (freqHeap.lastKey() < neighFreq) {
//
//                }
//                if (!freqHeap.containsKey(neighFreq)) {
//                  freqHeap.p
//                  if (v1 > freqHeap.lastKey()) {
//                    freqHeap.pollLastEntry();
//                    freqHeap.put()
//                  }
//                }
//                return true;
//              }
//          );
//          return true;
//        }
//    );
  }

  void build(Path statsRoot) {
    // TODO: save statistics to disk
  }

}
