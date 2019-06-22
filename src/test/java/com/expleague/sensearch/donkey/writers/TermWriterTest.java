package com.expleague.sensearch.donkey.writers;

import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.PartOfSpeech;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.core.lemmer.Lemmer;
import com.expleague.sensearch.donkey.utils.ParsedTerm;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term;
import com.expleague.sensearch.utils.SensearchTestCase;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import com.google.protobuf.InvalidProtocolBufferException;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.junit.Assert;
import org.junit.Test;

public class TermWriterTest extends SensearchTestCase {

  private List<WordInfo> createWordInfo(String text, String lemma, PartOfSpeech pos) {
    if (lemma == null) {
      return Collections.singletonList(new WordInfo(CharSeq.intern(text), new ArrayList<>()));
    }
    return Collections.singletonList(
        new WordInfo(
            CharSeq.intern(text),
            Collections.singletonList(new LemmaInfo(CharSeq.intern(lemma), 1, pos))));
  }

  private final Set<String> knownWords = Sets.newHashSet("cats", "dogs", "walked", "walk",
      "greatest", "kekekekeke", "walking", "catsss");
  private final Lemmer lemmerStub = new Lemmer(null, null) {
    @Override
    public List<WordInfo> parse(CharSequence seq) {
      switch (seq.toString()) {
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
          return createWordInfo("walking", "walk", PartOfSpeech.V);

        case "catsss":
          return createWordInfo("catsss", "cat", PartOfSpeech.S);

        default:
          throw new IllegalArgumentException("Unexpected token " + seq);
      }
    }
  };

  @Test
  public void testWriter() throws IOException {
    Path termWriterRoot = testOutputRoot().resolve(TermWriterTest.class.getName());
    TermWriter termWriter = new TermWriter(termWriterRoot);
    TLongSet wordIds = new TLongHashSet();
    for (String word : knownWords) {
      ParsedTerm parsedTerm = ParsedTerm.parse(word, lemmerStub);
      wordIds.add(parsedTerm.wordId());
      wordIds.add(parsedTerm.lemmaId());
      termWriter.write(parsedTerm);
    }
    termWriter.close();
    DB termDb = JniDBFactory.factory.open(termWriterRoot.toFile(), dbOpenOptions());
    DBIterator termIterator = termDb.iterator();
    termIterator.seekToFirst();
    final int[] baseSize = new int[]{0};
    final List<Term> termsFromBase = new ArrayList<>();
    termIterator.forEachRemaining(item -> {
      long wordId = Longs.fromByteArray(item.getKey());
      Assert.assertTrue("Unknown id in the base!", wordIds.contains(wordId));
      Term term;
      try {
        term = Term.parseFrom(item.getValue());
        baseSize[0]++;
      } catch (InvalidProtocolBufferException e) {
        throw new RuntimeException(e);
      }
      termsFromBase.add(term);
    });

    // unique words count + unique lemmas count
    Assert.assertEquals("Incomplete base", 12, baseSize[0]);
    Set<String> wordsFromBase =
        termsFromBase.stream()
            .filter(t -> t.hasPartOfSpeech() && t.getPartOfSpeech() != Term.PartOfSpeech.UNKNOWN)
            .map(Term::getText)
            .collect(Collectors.toSet());
    wordsFromBase.removeAll(knownWords);
    Assert.assertTrue(wordsFromBase.isEmpty());
  }
}
