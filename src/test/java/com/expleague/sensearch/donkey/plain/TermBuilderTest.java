package com.expleague.sensearch.donkey.plain;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.PartOfSpeech;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.donkey.plain.TermBuilder.TermAndLemmaIdPair;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term;
import com.google.common.primitives.Longs;
import com.google.protobuf.InvalidProtocolBufferException;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TermBuilderTest {

  private static final Path TERM_DB_PATH = Paths.get("testTermDbPath");

  @Before
  public void beforeTest() throws IOException {
    Files.createDirectories(TERM_DB_PATH);
  }

  @After
  public void afterTest() throws IOException {
    FileUtils.deleteDirectory(TERM_DB_PATH.toFile());
  }

  @Test
  public void testParseTerms() throws IOException {

    // SetUp
    String[] words = {
        "cats", "dogs", "walked", "walk", "kekekekeke", "greatest", "walking", "catsss",
    };

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

            case "walking":
              return createWordInfo("walk", "walk", PartOfSpeech.V);

            case "catsss":
              return createWordInfo("catsss", "cat", PartOfSpeech.S);

            default:
              throw new IllegalArgumentException("Unexpected token " + charSequence);
          }
        };

    Map<String, Long> wordToReturnedTermId = new HashMap<>();
    Map<String, Long> wordToReturnedLemmaId = new HashMap<>();
    try (TermBuilder termBuilder =
        new TermBuilder(
            JniDBFactory.factory.open(TERM_DB_PATH.toFile(), new Options().errorIfExists(true)),
            new Lemmer(fakeMyStem), new IdGenerator())) {

      for (String word : words) {
        TermAndLemmaIdPair termAndLemmaIdPair = termBuilder.addTerm(word);
        wordToReturnedTermId.put(word, termAndLemmaIdPair.termId);
        wordToReturnedLemmaId.put(word, termAndLemmaIdPair.lemmaId);
      }
    }

    TLongObjectMap<IndexUnits.Term> terms = new TLongObjectHashMap<>();

    // Load built terms
    try (DB termDb = JniDBFactory.factory.open(TERM_DB_PATH.toFile(), new Options())) {
      DBIterator iterator = termDb.iterator();
      iterator.seekToFirst();
      iterator.forEachRemaining(
          item -> {
            try {
              IndexUnits.Term term = IndexUnits.Term.parseFrom(item.getValue());
              terms.put(Longs.fromByteArray(item.getKey()), term);
            } catch (InvalidProtocolBufferException e) {
              e.printStackTrace();
              throw new RuntimeException(e);
            }
          });
    }

    Map<String, IndexUnits.Term> textToTerm = new HashMap<>();
    terms.forEachValue(term -> {
      textToTerm.put(term.getText(), term);
      return true;
    });

    // Test
    // Check that all terms are created and stored into DB
    Set<Long> termsIds =
        terms.valueCollection().stream().map(Term::getId).collect(Collectors.toSet());
    assertEquals(11, terms.size());
    for (String word : words) {
      assertTrue(textToTerm.containsKey(word));
    }
    assertTrue(textToTerm.containsKey("dog"));
    assertTrue(textToTerm.containsKey("cat"));
    assertTrue(textToTerm.containsKey("great"));

    // Check that lemma ids and pos are correct
    assertEquals(textToTerm.get("cat").getId(), textToTerm.get("cats").getLemmaId());
    assertEquals(Term.PartOfSpeech.S, textToTerm.get("cats").getPartOfSpeech());

    assertEquals(textToTerm.get("dog").getId(), textToTerm.get("dogs").getLemmaId());
    assertEquals(Term.PartOfSpeech.S, textToTerm.get("dogs").getPartOfSpeech());

    assertEquals(textToTerm.get("walk").getId(), textToTerm.get("walked").getLemmaId());
    assertEquals(Term.PartOfSpeech.V, textToTerm.get("walked").getPartOfSpeech());

    assertEquals(-1, textToTerm.get("walk").getLemmaId());
    assertEquals(Term.PartOfSpeech.V, textToTerm.get("walked").getPartOfSpeech());

    assertEquals(-1, textToTerm.get("kekekekeke").getLemmaId());
    assertFalse(textToTerm.get("kekekekeke").hasPartOfSpeech());

    assertEquals(textToTerm.get("great").getId(), textToTerm.get("greatest").getLemmaId());
    assertEquals(Term.PartOfSpeech.A, textToTerm.get("greatest").getPartOfSpeech());

    assertEquals(textToTerm.get("walk").getId(), textToTerm.get("walking").getLemmaId());
    assertEquals(Term.PartOfSpeech.V, textToTerm.get("walking").getPartOfSpeech());

    assertEquals(textToTerm.get("cat").getId(), textToTerm.get("catsss").getLemmaId());
    assertEquals(Term.PartOfSpeech.S, textToTerm.get("catsss").getPartOfSpeech());

    assertEquals(-1, textToTerm.get("cat").getLemmaId());
    assertEquals(Term.PartOfSpeech.S, textToTerm.get("cat").getPartOfSpeech());

    assertEquals(-1, textToTerm.get("dog").getLemmaId());
    assertEquals(Term.PartOfSpeech.S, textToTerm.get("dog").getPartOfSpeech());

    // Check that TermBuilder.addTerm returns the same ids that are used for storage
    for (String word : words) {
      IndexUnits.Term term = textToTerm.get(word);
      assertEquals(term.getId(), wordToReturnedTermId.get(term.getText()).longValue());
      assertEquals(term.getLemmaId(), wordToReturnedLemmaId.get(term.getText()).longValue());
    }
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
