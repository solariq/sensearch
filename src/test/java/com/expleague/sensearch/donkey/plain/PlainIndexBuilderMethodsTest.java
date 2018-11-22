package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.utils.SensearchTestCase;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.nio.file.Path;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class PlainIndexBuilderMethodsTest extends SensearchTestCase {


  private static final PlainIndexBuilder PLAIN_INDEX_BUILDER = new PlainIndexBuilder();

  @Test
  public void enrichFrequenciesTest() {
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
  public void flushIdMappingsTest() {
    Path testOutputRoot = testOutputRoot();

  }

  @Test
  public void readGloveVectorsTest() {

  }

  @Test
  public void toVectorTest() {

  }
}
