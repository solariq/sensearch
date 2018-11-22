package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.utils.SensearchTestCase;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import org.junit.Assert;
import org.junit.Test;

public class PlainIndexBuilderMethodsTest extends SensearchTestCase {


  private static final PlainIndexBuilder PLAIN_INDEX_BUILDER = new PlainIndexBuilder();

  @Test
  public void enrichFrequenciesTest() {
    long[] idSequence = new long[] {
        1, 2, 3, 1, 2, 1, 3, 2, 3, 3, 2, 3, 1
    };
    TLongIntMap frequenciesMap = new TLongIntHashMap();
    TLongObjectMap<TLongIntMap> bigramsMap = new TLongObjectHashMap<>();

    PLAIN_INDEX_BUILDER.enrichFrequencies(idSequence, frequenciesMap, bigramsMap);
    // test frequencies
    Assert.assertFalse(frequenciesMap.isEmpty());
    Assert.assertEquals(frequenciesMap.size(), 3);
    Assert.assertEquals(frequenciesMap.get(1), 4);
    Assert.assertEquals(frequenciesMap.get(2), 4);
    Assert.assertEquals(frequenciesMap.get(3), 5);

    // test bigrams
    Assert.assertFalse(bigramsMap.isEmpty());
    Assert.assertEquals(bigramsMap.size(), 3);

    TLongIntMap bigramsFor1 = bigramsMap.get(1);
    Assert.assertFalse(bigramsFor1.isEmpty());
    Assert.assertFalse(bigramsFor1.containsKey(1));
    Assert.assertEquals(bigramsFor1.get(2), 2);
    Assert.assertEquals(bigramsFor1.get(3), 1);
  }

  @Test
  public void toWordIdsTest() {
    TObjectLongMap<String> knownIdMappings = new TObjectLongHashMap<>();
    knownIdMappings.put("word1", 1);
    knownIdMappings.put("word2", 2);
    knownIdMappings.put("word3", 3);

    long[] convertResult;

    // Simple test
    convertResult = PLAIN_INDEX_BUILDER.toIds(
        new String[]{"word1", "word3"}, knownIdMappings
    );
    Assert.assertArrayEquals(convertResult, new long[]{1, 3});

    // Empty input test
    convertResult = PLAIN_INDEX_BUILDER.toIds(
        new String[0], knownIdMappings
    );
    Assert.assertEquals(convertResult.length, 0);

    // New words test
    convertResult = PLAIN_INDEX_BUILDER.toIds(
        new String[]{"word4"}, knownIdMappings
    );
    Assert.assertTrue(knownIdMappings.containsKey("word1"));
    Assert.assertTrue(knownIdMappings.containsKey("word2"));
    Assert.assertTrue(knownIdMappings.containsKey("word3"));
    Assert.assertTrue(knownIdMappings.containsKey("word4"));
    Assert.assertArrayEquals(convertResult, new long[]{4});
    Assert.assertEquals(knownIdMappings.get("word4"), 4);

    // Empty mappings test
    knownIdMappings.clear();
    convertResult = PLAIN_INDEX_BUILDER.toIds(
        new String[]{"word1", "word2", "word3"}, knownIdMappings
    );
    Assert.assertTrue(knownIdMappings.containsKey("word1"));
    Assert.assertTrue(knownIdMappings.containsKey("word2"));
    Assert.assertTrue(knownIdMappings.containsKey("word3"));
    Assert.assertFalse(knownIdMappings.containsKey("word4"));
    Assert.assertEquals(knownIdMappings.get("word1"), 1);
    Assert.assertEquals(knownIdMappings.get("word2"), 2);
    Assert.assertEquals(knownIdMappings.get("word3"), 3);
    Assert.assertArrayEquals(convertResult, new long[]{1, 2, 3});
  }

  @Test
  public void flushIdMappingsTest() throws IOException {
    Path testOutputRoot = testOutputRoot().resolve("IdMappingFlushTest");
    Files.createDirectories(testOutputRoot);
    Path testOutputFile = testOutputRoot.resolve("mappings.gz");
    TObjectLongMap<String> knowIdMappings = new TObjectLongHashMap<>();
    knowIdMappings.put("word1", 1);
    knowIdMappings.put("word2", 2);
    knowIdMappings.put("word3", 3);

    PLAIN_INDEX_BUILDER.flushIdMappings(testOutputFile, knowIdMappings);
    Assert.assertTrue(Files.exists(testOutputFile));

    BufferedReader bufferedReader = new BufferedReader(
        new InputStreamReader(
            new GZIPInputStream(Files.newInputStream(testOutputFile))
        )
    );

    TObjectLongMap<String> serializedMappings = new TObjectLongHashMap<>();
    bufferedReader.lines().forEach(
        l -> {
          String[] tokens = l.split("\t");
          serializedMappings.put(tokens[0], Long.parseLong(tokens[1]));
        }
    );

    Assert.assertEquals(serializedMappings.size(), knowIdMappings.size());
    knowIdMappings.forEachEntry(
        (k, v) -> {
          Assert.assertTrue(serializedMappings.containsKey(k));
          Assert.assertEquals(serializedMappings.get(k), v);
          return true;
        }
    );
  }

  @Test
  public void readGloveVectorsTest() {

  }

  @Test
  public void toVectorTest() {

  }
}
