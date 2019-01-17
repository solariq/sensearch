package com.expleague.sensearch.donkey.plain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics.TermFrequency;
import com.expleague.sensearch.utils.SensearchTestCase;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.google.protobuf.InvalidProtocolBufferException;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StatisticsBuilderTest extends SensearchTestCase {

  private static final Path STATS_DB_PATH = Paths.get("testStatsDbPath");

  @Before
  public void beforeTest() throws IOException {
    Files.createDirectories(STATS_DB_PATH);
  }

  @After
  public void afterTest() throws IOException {
    FileUtils.deleteDirectory(STATS_DB_PATH.toFile());
  }

  @Test
  public void test() throws IOException {
    try (StatisticsBuilder statisticsBuilder =
        new StatisticsBuilder(
            JniDBFactory.factory.open(STATS_DB_PATH.toFile(), new Options().errorIfExists(true)))) {

      statisticsBuilder.startPage();
      for (long id : new long[]{1, 3, 2, 4, 3, 3, 2, 3, 1, 5}) {
        statisticsBuilder.enrich(id, -1);
      }
      statisticsBuilder.endPage();

      statisticsBuilder.startPage();
      for (long id : new long[]{6, 7, 8, 8, 2, 7, 3}) {
        statisticsBuilder.enrich(id, -1);
      }
      statisticsBuilder.endPage();
    }

    Map<Long, TermStatistics> termStatistics = new HashMap<>();
    try (DB statsDb = JniDBFactory.factory.open(STATS_DB_PATH.toFile(), new Options())) {
      DBIterator iterator = statsDb.iterator();
      iterator.seekToFirst();
      iterator.forEachRemaining(
          entry -> {
            try {
              termStatistics.put(
                  Longs.fromByteArray(entry.getKey()), TermStatistics.parseFrom(entry.getValue()));
            } catch (InvalidProtocolBufferException e) {
              throw new RuntimeException(e);
            }
          });
    }

    assertEquals(8, termStatistics.size());

    TermStatistics term1 = termStatistics.get(1L);
    assertEquals(1, term1.getTermId());
    assertEquals(2, term1.getTermFrequency());
    assertEquals(1, term1.getDocumentFrequency());
    checkTermFreqs(Arrays.asList(0, 0, 1, 0, 1, 0, 0, 0), term1.getBigramFrequencyList());

    TermStatistics term2 = termStatistics.get(2L);
    assertEquals(2, term2.getTermId());
    assertEquals(3, term2.getTermFrequency());
    assertEquals(2, term2.getDocumentFrequency());
    checkTermFreqs(Arrays.asList(0, 0, 1, 1, 0, 0, 1, 0), term2.getBigramFrequencyList());

    TermStatistics term3 = termStatistics.get(3L);
    assertEquals(3, term3.getTermId());
    assertEquals(5, term3.getTermFrequency());
    assertEquals(2, term3.getDocumentFrequency());
    checkTermFreqs(Arrays.asList(1, 2, 1, 0, 0, 0, 0, 0), term3.getBigramFrequencyList());

    TermStatistics term4 = termStatistics.get(4L);
    assertEquals(4, term4.getTermId());
    assertEquals(1, term4.getTermFrequency());
    assertEquals(1, term4.getDocumentFrequency());
    checkTermFreqs(Arrays.asList(0, 0, 1, 0, 0, 0, 0, 0), term4.getBigramFrequencyList());

    TermStatistics term5 = termStatistics.get(5L);
    assertEquals(5, term5.getTermId());
    assertEquals(1, term5.getTermFrequency());
    assertEquals(1, term5.getDocumentFrequency());
    checkTermFreqs(Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0), term5.getBigramFrequencyList());

    TermStatistics term6 = termStatistics.get(6L);
    assertEquals(6, term6.getTermId());
    assertEquals(1, term6.getTermFrequency());
    assertEquals(1, term6.getDocumentFrequency());
    checkTermFreqs(Arrays.asList(0, 0, 0, 0, 0, 0, 1, 0), term6.getBigramFrequencyList());

    TermStatistics term7 = termStatistics.get(7L);
    assertEquals(7, term7.getTermId());
    assertEquals(2, term7.getTermFrequency());
    assertEquals(1, term7.getDocumentFrequency());
    checkTermFreqs(Arrays.asList(0, 0, 1, 0, 0, 0, 0, 1), term7.getBigramFrequencyList());

    TermStatistics term8 = termStatistics.get(8L);
    assertEquals(8, term8.getTermId());
    assertEquals(2, term8.getTermFrequency());
    assertEquals(1, term8.getDocumentFrequency());
    checkTermFreqs(Arrays.asList(0, 1, 0, 0, 0, 0, 0, 1), term8.getBigramFrequencyList());
  }

  private void checkTermFreqs(List<Integer> bigrams, List<TermFrequency> bigramFrequencyList) {
    // Check that each bigram has the expected frequency
    bigramFrequencyList.forEach(
        termFreq ->
            assertEquals(
                bigrams.get((int) termFreq.getTermId() - 1).intValue(),
                termFreq.getTermFrequency()));

    Map<Long, Integer> freqMap = bigramFrequencyList.stream()
        .collect(Collectors.toMap(TermFrequency::getTermId, TermFrequency::getTermFrequency));

    // Check that there are no extra bigrams
    for (int i = 0; i < bigrams.size(); i++) {
      int cnt = bigrams.get(i);
      if (cnt == 0) {
        assertFalse(freqMap.containsKey(i + 1L));
      } else {
        assertEquals(cnt, freqMap.get(i + 1L).intValue());
      }
    }
  }

  @Test
  public void mostFrequentBigramsTest() throws IOException {
    TLongIntMap neighboursFreq = new TLongIntHashMap();
    neighboursFreq.put(1, 1);
    neighboursFreq.put(2, 3);
    neighboursFreq.put(3, 5);
    neighboursFreq.put(4, 7);
    neighboursFreq.put(5, 5);
    neighboursFreq.put(6, 3);
    neighboursFreq.put(7, 1);

    Iterable<TermFrequency> mostFreq = StatisticsBuilder.mostFrequentBigrams(neighboursFreq, 3);
    List<TermFrequency> mFreqMap = Lists.newArrayList(mostFreq);
    assertEquals(3, mFreqMap.size());
    assertEquals(4, mFreqMap.get(0).getTermId());
    assertEquals(7, mFreqMap.get(0).getTermFrequency());

    assertEquals(5, mFreqMap.get(1).getTermFrequency());
    assertEquals(5, mFreqMap.get(2).getTermFrequency());
  }

  @Test
  public void mostFreqSameFreqTest() {
    TLongIntMap neighboursFreq = new TLongIntHashMap();
    neighboursFreq.put(1, 8);
    neighboursFreq.put(2, 8);
    neighboursFreq.put(3, 8);
    neighboursFreq.put(4, 8);
    neighboursFreq.put(5, 8);

    ArrayList<TermFrequency> freqNeigh =
        Lists.newArrayList(StatisticsBuilder.mostFrequentBigrams(neighboursFreq, 3));

    assertEquals(3, freqNeigh.size());
    freqNeigh.forEach(
        tf -> {
          Assert.assertTrue(neighboursFreq.containsKey(tf.getTermId()));
          assertEquals(neighboursFreq.get(tf.getTermId()), tf.getTermFrequency());
        });
  }
}
