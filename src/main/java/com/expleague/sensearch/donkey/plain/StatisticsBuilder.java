package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;

class StatisticsBuilder {

  private static final int MOST_FREQUENT_BIGRAMS_COUNT = 10;
  private final TObjectIntMap<String> wordToIdMappings = new TObjectIntHashMap<>();

  private final TLongLongMap wordFrequencyMap = new TLongLongHashMap();
  private final TLongIntMap documentFrequencyMap = new TLongIntHashMap();
  private final TIntObjectMap<int[]> bigramsMap = new TIntObjectHashMap<>();
  // docsAndWordsCounts[0] -- current documents count
  // docsAndWordsCounts[1] -- overall tokens count in observed documents
  private final long[] docsAndWordsCounts = new long[2];

  private final Config config;

  StatisticsBuilder(Config config) {
    this.config = config;
  }

  void enrichStatistics(CrawlerDocument crawlerDocument) {
    ++docsAndWordsCounts[0];
    final TIntObjectMap<TIntIntMap> largeBigramsMap = new TIntObjectHashMap<>();
    int[] prevTokenId = new int[]{-1};
    Stream.of(Tokenizer
        .tokenize((crawlerDocument.getTitle() + " " + crawlerDocument.getContent()).toLowerCase()))
        .filter(s -> !s.isEmpty())
        .forEach(
            token -> {
              ++docsAndWordsCounts[1];
              TIntSet uniqueTerms = new TIntHashSet();
              wordToIdMappings.putIfAbsent(token, wordFrequencyMap.size());
              int wordId = wordToIdMappings.get(token);
              uniqueTerms.add(wordId);
              wordFrequencyMap.adjustOrPutValue(wordId, 1, 1);
              uniqueTerms.forEach(
                  id -> {
                    documentFrequencyMap.adjustOrPutValue(id, 1, 1);
                    return true;
                  }
              );
              largeBigramsMap.putIfAbsent(wordId, new TIntIntHashMap());
              if (prevTokenId[0] >= 0) {
                largeBigramsMap.get(wordId).adjustOrPutValue(prevTokenId[0], 1, 1);
              }
              prevTokenId[0] = wordId;
            }
        );

  }

  private void trimBigrams(TIntObjectMap<TIntIntMap> largeBigramsMap) {
    final NavigableMap<Integer, TIntLinkedList> freqHeap = new TreeMap<>(Comparator.reverseOrder());
    int[] currentMapSize = new int[]{0};
    largeBigramsMap.forEachEntry(
        (tokId, neighMap) -> {
          neighMap.forEachEntry(
              (neighId, neighFreq) -> {
                freqHeap.putIfAbsent(neighFreq, new TIntLinkedList());
                if (currentMapSize[0] < MOST_FREQUENT_BIGRAMS_COUNT) {
                  ++currentMapSize[0];
                } else if (freqHeap.lastKey() < neighFreq) {

                }
                if (!freqHeap.containsKey(neighFreq)) {
                  freqHeap.p
                  if (v1 > freqHeap.lastKey()) {
                    freqHeap.pollLastEntry();
                    freqHeap.put()
                  }
                }
                return true;
              }
          );
          return true;
        }
    );
  }

  void flushStatics() {
    // TODO: save statistics to disk
  }

  TObjectIntMap<String> wordToIntMappings() {
    return wordToIdMappings;
  }
}
