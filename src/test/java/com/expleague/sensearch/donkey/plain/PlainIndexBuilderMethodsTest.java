//package com.expleague.sensearch.donkey.plain;
//
//import static junit.framework.TestCase.assertTrue;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNull;
//
//import com.expleague.commons.math.vectors.Vec;
//import com.expleague.commons.seq.CharSeq;
//import com.expleague.commons.text.lemmer.LemmaInfo;
//import com.expleague.commons.text.lemmer.MyStem;
//import com.expleague.commons.text.lemmer.PartOfSpeech;
//import com.expleague.commons.text.lemmer.WordInfo;
//import com.expleague.sensearch.core.Lemmer;
//import com.expleague.sensearch.donkey.plain.PlainIndexBuilder.ParsedTerm;
//import com.expleague.sensearch.utils.SensearchTestCase;
//import gnu.trove.map.TLongObjectMap;
//import gnu.trove.map.TObjectLongMap;
//import gnu.trove.map.hash.TLongObjectHashMap;
//import gnu.trove.map.hash.TObjectLongHashMap;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//import java.util.stream.LongStream;
//import java.util.stream.Stream;
//import org.junit.Assert;
//import org.junit.Ignore;
//import org.junit.Test;
//
//public class PlainIndexBuilderMethodsTest extends SensearchTestCase {
//
////  @Test
////  public void toWordIdsTest() {
////    TObjectLongMap<String> knownIdMappings = new TObjectLongHashMap<>();
////    knownIdMappings.put("word1", 1);
////    knownIdMappings.put("word2", 2);
////    knownIdMappings.put("word3", 3);
////
////    long[] convertResult;
////
////    // Simple test
////    convertResult = PlainIndexBuilder.toIds(Stream.of("word1", "word3"), knownIdMappings);
////    Assert.assertArrayEquals(new long[] {1, 3}, convertResult);
////
////    // Empty input test
////    convertResult = PlainIndexBuilder.toIds(Stream.empty(), knownIdMappings);
////    assertEquals(convertResult.length, 0);
////
////    // New words test
////    convertResult = PlainIndexBuilder.toIds(Stream.of("word4"), knownIdMappings);
////    Assert.assertTrue(knownIdMappings.containsKey("word1"));
////    Assert.assertTrue(knownIdMappings.containsKey("word2"));
////    Assert.assertTrue(knownIdMappings.containsKey("word3"));
////    Assert.assertTrue(knownIdMappings.containsKey("word4"));
////    Assert.assertArrayEquals(new long[] {4}, convertResult);
////    assertEquals(4, knownIdMappings.get("word4"));
////
////    // Empty mappings test
////    knownIdMappings.clear();
////    convertResult = PlainIndexBuilder.toIds(Stream.of("word1", "word2", "word3"), knownIdMappings);
////    Assert.assertTrue(knownIdMappings.containsKey("word1"));
////    Assert.assertTrue(knownIdMappings.containsKey("word2"));
////    Assert.assertTrue(knownIdMappings.containsKey("word3"));
////    Assert.assertFalse(knownIdMappings.containsKey("word4"));
////    assertEquals(1, knownIdMappings.get("word1"));
////    assertEquals(2, knownIdMappings.get("word2"));
////    assertEquals(3, knownIdMappings.get("word3"));
////    Assert.assertArrayEquals(convertResult, new long[] {1, 2, 3});
////  }
//
//  @Test
//  @Ignore
//  public void readGloveVectorsTest() {
//    // TODO: make test for reading glove vectors
//  }
//
//  @Test
//  @Ignore
//  public void toVectorTest() {
//    // TODO: test to vector conversion
//    TLongObjectMap<Vec> knownVectors = new TLongObjectHashMap<>();
//  }
//}
