package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta.IdMapping;
import com.google.common.collect.Lists;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class PlainIndexBuilderMethodsTest {

  @Test
  public void enrichFrequenciesTest() {
    long[] idSequence = new long[] {1, 2, 3, 1, 2, 1, 3, 2, 3, 3, 2, 3, 1};
    TLongIntMap frequenciesMap = new TLongIntHashMap();
    TLongObjectMap<TLongIntMap> bigramsMap = new TLongObjectHashMap<>();

    PlainIndexBuilder.enrichFrequencies(idSequence, frequenciesMap, bigramsMap);
    // test frequencies
    Assert.assertFalse(frequenciesMap.isEmpty());
    Assert.assertEquals(3, frequenciesMap.size());
    Assert.assertEquals(4, frequenciesMap.get(1));
    Assert.assertEquals(4, frequenciesMap.get(2));
    Assert.assertEquals(5, frequenciesMap.get(3));

    // test bigrams
    Assert.assertFalse(bigramsMap.isEmpty());
    Assert.assertEquals(bigramsMap.size(), 3);

    TLongIntMap bigramsFor1 = bigramsMap.get(1);
    Assert.assertFalse(bigramsFor1.isEmpty());
    Assert.assertFalse(bigramsFor1.containsKey(1));
    Assert.assertEquals(2, bigramsFor1.get(2));
    Assert.assertEquals(1, bigramsFor1.get(3));
  }

  @Test
  public void toWordIdsTest() {
    TObjectLongMap<String> knownIdMappings = new TObjectLongHashMap<>();
    knownIdMappings.put("word1", 1);
    knownIdMappings.put("word2", 2);
    knownIdMappings.put("word3", 3);

    long[] convertResult;

    // Simple test
    convertResult = PlainIndexBuilder.toIds(new String[] {"word1", "word3"}, knownIdMappings);
    Assert.assertArrayEquals(new long[] {1, 3}, convertResult);

    // Empty input test
    convertResult = PlainIndexBuilder.toIds(new String[0], knownIdMappings);
    Assert.assertEquals(convertResult.length, 0);

    // New words test
    convertResult = PlainIndexBuilder.toIds(new String[] {"word4"}, knownIdMappings);
    Assert.assertTrue(knownIdMappings.containsKey("word1"));
    Assert.assertTrue(knownIdMappings.containsKey("word2"));
    Assert.assertTrue(knownIdMappings.containsKey("word3"));
    Assert.assertTrue(knownIdMappings.containsKey("word4"));
    Assert.assertArrayEquals(new long[] {4}, convertResult);
    Assert.assertEquals(4, knownIdMappings.get("word4"));

    // Empty mappings test
    knownIdMappings.clear();
    convertResult =
        PlainIndexBuilder.toIds(new String[] {"word1", "word2", "word3"}, knownIdMappings);
    Assert.assertTrue(knownIdMappings.containsKey("word1"));
    Assert.assertTrue(knownIdMappings.containsKey("word2"));
    Assert.assertTrue(knownIdMappings.containsKey("word3"));
    Assert.assertFalse(knownIdMappings.containsKey("word4"));
    Assert.assertEquals(1, knownIdMappings.get("word1"));
    Assert.assertEquals(2, knownIdMappings.get("word2"));
    Assert.assertEquals(3, knownIdMappings.get("word3"));
    Assert.assertArrayEquals(convertResult, new long[] {1, 2, 3});
  }

  @Test
  public void idMappingsToProtobufTest() {
    TObjectLongMap<String> knowIdMappings = new TObjectLongHashMap<>();
    for (int i = 0; i < 10; ++i) {
      knowIdMappings.put("word" + i, i);

      Iterable<IdMapping> protobufMapings = PlainIndexBuilder.toProtobufIterable(knowIdMappings);

      protobufMapings.forEach(
          m -> {
            Assert.assertTrue(knowIdMappings.containsKey(m.getWord()));
            Assert.assertEquals(knowIdMappings.get(m.getWord()), m.getId());
          });

      Assert.assertEquals(knowIdMappings.size(), Lists.newArrayList(protobufMapings).size());
    }
  }

  @Test
  @Ignore
  public void readGloveVectorsTest() {
    // TODO: make test for reading glove vectors
  }

  @Test
  @Ignore
  public void toVectorTest() {
    // TODO: test to vector conversion
    TLongObjectMap<Vec> knownVectors = new TLongObjectHashMap<>();
  }
}
