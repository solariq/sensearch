package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.protobuf.index.IndexUnits.TermStatistics.TermFrequency;
import com.expleague.sensearch.utils.SensearchTestCase;
import com.google.common.collect.Lists;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class StatisticsBuilderMethodsTest extends SensearchTestCase {

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
    Assert.assertEquals(3, mFreqMap.size());
    Assert.assertEquals(4, mFreqMap.get(0).getTermId());
    Assert.assertEquals(7, mFreqMap.get(0).getTermFrequency());

    Assert.assertEquals(5, mFreqMap.get(1).getTermFrequency());
    Assert.assertEquals(5, mFreqMap.get(2).getTermFrequency());
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

    Assert.assertEquals(3, freqNeigh.size());
    freqNeigh.forEach(
        tf -> {
          Assert.assertTrue(neighboursFreq.containsKey(tf.getTermId()));
          Assert.assertEquals(neighboursFreq.get(tf.getTermId()), tf.getTermFrequency());
        });
  }
}
