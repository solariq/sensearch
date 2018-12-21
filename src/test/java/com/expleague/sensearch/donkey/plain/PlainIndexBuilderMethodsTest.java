package com.expleague.sensearch.donkey.plain;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.PartOfSpeech;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder.ParsedTerm;
import com.expleague.sensearch.utils.SensearchTestCase;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class PlainIndexBuilderMethodsTest extends SensearchTestCase {

  @Test
  public void toWordIdsTest() {
    TObjectLongMap<String> knownIdMappings = new TObjectLongHashMap<>();
    knownIdMappings.put("word1", 1);
    knownIdMappings.put("word2", 2);
    knownIdMappings.put("word3", 3);

    long[] convertResult;

    // Simple test
    convertResult = PlainIndexBuilder.toIds(Stream.of("word1", "word3"), knownIdMappings);
    Assert.assertArrayEquals(new long[] {1, 3}, convertResult);

    // Empty input test
    convertResult = PlainIndexBuilder.toIds(Stream.empty(), knownIdMappings);
    assertEquals(convertResult.length, 0);

    // New words test
    convertResult = PlainIndexBuilder.toIds(Stream.of("word4"), knownIdMappings);
    Assert.assertTrue(knownIdMappings.containsKey("word1"));
    Assert.assertTrue(knownIdMappings.containsKey("word2"));
    Assert.assertTrue(knownIdMappings.containsKey("word3"));
    Assert.assertTrue(knownIdMappings.containsKey("word4"));
    Assert.assertArrayEquals(new long[] {4}, convertResult);
    assertEquals(4, knownIdMappings.get("word4"));

    // Empty mappings test
    knownIdMappings.clear();
    convertResult = PlainIndexBuilder.toIds(Stream.of("word1", "word2", "word3"), knownIdMappings);
    Assert.assertTrue(knownIdMappings.containsKey("word1"));
    Assert.assertTrue(knownIdMappings.containsKey("word2"));
    Assert.assertTrue(knownIdMappings.containsKey("word3"));
    Assert.assertFalse(knownIdMappings.containsKey("word4"));
    assertEquals(1, knownIdMappings.get("word1"));
    assertEquals(2, knownIdMappings.get("word2"));
    assertEquals(3, knownIdMappings.get("word3"));
    Assert.assertArrayEquals(convertResult, new long[] {1, 2, 3});
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

  @Test
  public void testParseTerms() {
    TObjectLongMap<String> idMapping = new TObjectLongHashMap<>();
    idMapping.put("cats", 1);
    idMapping.put("dogs", 2);
    idMapping.put("walked", 3);
    idMapping.put("walk", 4);
    idMapping.put("kekekekeke", 5);
    idMapping.put("greatest", 6);

    MyStem fakeMyStem =
        charSequence -> {
          switch (charSequence.toString()) {
            case "cats":
              return createWordInfo("cats", "cat", PartOfSpeech.S);

            case "dogs":
              return createWordInfo("dogs", "dog", PartOfSpeech.S);

            case "walked":
              return createWordInfo("walked", "walk", PartOfSpeech.V);

            case "walk":
              return createWordInfo("walk", "walk", PartOfSpeech.V);

            case "greatest":
              return createWordInfo("greatest", "great", PartOfSpeech.A);

            case "kekekekeke":
              return createWordInfo("kekekekeke", null, null);

            default:
              throw new IllegalArgumentException("Unexpected token " + charSequence);
          }
        };

    TLongObjectMap<ParsedTerm> terms =
        PlainIndexBuilder.parseTerms(idMapping, new Lemmer(fakeMyStem));
    Map<Long, String> idToString = new HashMap<>();
    idMapping.forEachEntry(
        (k, v) -> {
          idToString.put(v, k);
          return true;
        });

    assertEquals(1, idMapping.get("cats"));
    assertEquals(2, idMapping.get("dogs"));
    assertEquals(3, idMapping.get("walked"));
    assertEquals(4, idMapping.get("walk"));
    assertEquals(5, idMapping.get("kekekekeke"));
    assertEquals(6, idMapping.get("greatest"));

    assertTrue(idMapping.containsKey("cat"));
    assertTrue(idMapping.containsKey("dog"));
    assertTrue(idMapping.containsKey("great"));

    assertEquals(9, idMapping.size());
    assertEquals(
        "All ids must be different", 9, LongStream.of(idMapping.values()).distinct().count());

    Set<Long> termsIds = terms.valueCollection().stream().map(term -> term.id)
        .collect(Collectors.toSet());
    for (int i = 1; i <= 9; i++) {
      assertTrue(termsIds.contains((long) i));
    }
    assertEquals(9, terms.size());

    Map<String, ParsedTerm> parsedTermMap =
        terms
            .valueCollection()
            .stream()
            .collect(Collectors.toMap(term -> idToString.get(term.id), Function.identity()));

    assertEquals(idMapping.get("cat"), parsedTermMap.get("cats").lemmaId);
    assertEquals(com.expleague.sensearch.core.PartOfSpeech.S,
        parsedTermMap.get("cats").partOfSpeech);
    assertEquals(1, parsedTermMap.get("cats").id);

    assertEquals(idMapping.get("dog"), parsedTermMap.get("dogs").lemmaId);
    assertEquals(com.expleague.sensearch.core.PartOfSpeech.S,
        parsedTermMap.get("dogs").partOfSpeech);
    assertEquals(2, parsedTermMap.get("dogs").id);

    assertEquals(4, parsedTermMap.get("walked").lemmaId);
    assertEquals(com.expleague.sensearch.core.PartOfSpeech.V,
        parsedTermMap.get("walked").partOfSpeech);
    assertEquals(3, parsedTermMap.get("walked").id);

    assertEquals(-1, parsedTermMap.get("kekekekeke").lemmaId);
    assertNull(parsedTermMap.get("kekekekeke").partOfSpeech);
    assertEquals(5, parsedTermMap.get("kekekekeke").id);

    assertEquals(idMapping.get("great"), parsedTermMap.get("greatest").lemmaId);
    assertEquals(com.expleague.sensearch.core.PartOfSpeech.A,
        parsedTermMap.get("greatest").partOfSpeech);
    assertEquals(6, parsedTermMap.get("greatest").id);

    assertEquals(-1, parsedTermMap.get("cat").lemmaId);
    assertEquals(com.expleague.sensearch.core.PartOfSpeech.S,
        parsedTermMap.get("cat").partOfSpeech);

    assertEquals(-1, parsedTermMap.get("dog").lemmaId);
    assertEquals(com.expleague.sensearch.core.PartOfSpeech.S,
        parsedTermMap.get("dog").partOfSpeech);
  }

  private List<WordInfo> createWordInfo(String text, String lemma, PartOfSpeech pos) {
    if (lemma == null) {
      return Collections.singletonList(new WordInfo(CharSeq.intern(text), new ArrayList<>()));
    }
    return Collections.singletonList(
        new WordInfo(
            CharSeq.intern(text),
            Collections.singletonList(new LemmaInfo(CharSeq.intern(lemma), 1, pos))));
  }
}
