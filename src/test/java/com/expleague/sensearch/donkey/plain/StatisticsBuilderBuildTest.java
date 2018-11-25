package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics;
import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics.TermFrequency;
import com.expleague.sensearch.utils.SensearchTestCase;
import com.google.common.primitives.Longs;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.junit.Assert;
import org.junit.Test;

public class StatisticsBuilderBuildTest extends SensearchTestCase {

  private static final String DB_ROOT_NAME = "StatsDB";
  private static final long[][] WORD_ID_SEQ = new long[][]{
      {1, 2, 3, 2, 3, 1, 3, 2, 3, 1, 1},
      {2, 3, 1, 3, 2, 1, 4, 1, 2, 3, 1},
      {3, 2, 3, 4, 2, 4, 3, 2, 3, 4, 2}
  };
  private static Options DB_OPEN_OPTIONS = new Options()
      .cacheSize(1 << 10)
      .createIfMissing(false)
      .errorIfExists(false)
      .compressionType(CompressionType.SNAPPY);

  private static Options DB_CREATE_OPTIONS = new Options()
      .cacheSize(1 << 10)
      .createIfMissing(true)
      .errorIfExists(true)
      .compressionType(CompressionType.SNAPPY);

  @Test
  public void enrichFrequenciesTest() {
    TLongIntMap frequenciesMap = new TLongIntHashMap();
    TLongObjectMap<TLongIntMap> bigramsMap = new TLongObjectHashMap<>();

    PlainIndexBuilder.enrichFrequencies(WORD_ID_SEQ[0], frequenciesMap, bigramsMap);
    // test frequencies
    Assert.assertFalse(frequenciesMap.isEmpty());
    Assert.assertEquals(3, frequenciesMap.size());
    Assert.assertEquals(4, frequenciesMap.get(1));
    Assert.assertEquals(3, frequenciesMap.get(2));
    Assert.assertEquals(4, frequenciesMap.get(3));

    // test bigrams
    Assert.assertFalse(bigramsMap.isEmpty());
    Assert.assertEquals(bigramsMap.size(), 3);

    TLongIntMap bigramsFor1 = bigramsMap.get(1);
    Assert.assertFalse(bigramsFor1.isEmpty());
    Assert.assertEquals(1, bigramsFor1.get(1));
    Assert.assertEquals(1, bigramsFor1.get(2));
    Assert.assertEquals(1, bigramsFor1.get(3));
  }

  @Test
  public void simpleBuildTest() throws IOException {
    clearOutputRoot();
    enrichFrequenciesTest();

    Path statisticsOutputRoot = testOutputRoot().resolve(DB_ROOT_NAME);
    Files.createDirectories(statisticsOutputRoot);
    DB statisticsDb = JniDBFactory.factory.open(statisticsOutputRoot.toFile(),
        DB_CREATE_OPTIONS);

    TLongIntMap tFreqMap = new TLongIntHashMap();
    TLongObjectMap<TLongIntMap> bFreqMap = new TLongObjectHashMap<>();
    StatisticsBuilder statisticsBuilder = new StatisticsBuilder(statisticsDb);
    for (long[] seq : WORD_ID_SEQ) {
      tFreqMap.clear();
      bFreqMap.clear();
      PlainIndexBuilder.enrichFrequencies(seq, tFreqMap, bFreqMap);
      statisticsBuilder.enrich(tFreqMap, bFreqMap);
    }

    statisticsBuilder.build();
  }

  @Test
  public void StatsDbContentTest() throws IOException {
    clearOutputRoot();
    simpleBuildTest();
    DB statsDb = JniDBFactory.factory.open(testOutputRoot().resolve(DB_ROOT_NAME).toFile(),
        DB_OPEN_OPTIONS
    );

    TLongIntMap tFreqMap = new TLongIntHashMap();
    TLongObjectMap<TLongIntMap> bFreqMap = new TLongObjectHashMap<>();
    for (long[] wordSeq : WORD_ID_SEQ) {
      PlainIndexBuilder.enrichFrequencies(wordSeq, tFreqMap, bFreqMap);
    }

    TermStatistics statsFor1 = TermStatistics.parseFrom(statsDb.get(Longs.toByteArray(1)));
    Assert.assertEquals(tFreqMap.get(1), statsFor1.getTermFrequency());
    Assert.assertEquals(2, statsFor1.getDocuementFrequency());
    for (TermFrequency tf : statsFor1.getBigramFrequencyList()) {
      Assert.assertEquals(bFreqMap.get(1).get(tf.getTermId()), tf.getTermFrequency());
    }

    statsDb.close();
  }
}
