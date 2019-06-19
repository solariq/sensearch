package com.expleague.sensearch.donkey.utils;

import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.lemmer.Lemmer;
import java.util.List;
import javax.annotation.Nullable;

public class ParsedTerm {
  private static final String LEMMA_SUFFIX = "$";

  private final long wordId;
  private final CharSeq word;

  private final long lemmaId;
  private final CharSeq lemma;

  private final PartOfSpeech posTag;

  protected ParsedTerm(long wordId, CharSeq word,
      long lemmaId, CharSeq lemma,
      PartOfSpeech posTag) {
    this.wordId = wordId;
    this.word = word;
    this.lemmaId = lemmaId;
    this.lemma = lemma;
    this.posTag = posTag;
  }

  public static ParsedTerm parse(CharSequence wordcs, Lemmer lemmer, TokenParser parser) {
    CharSeq word = CharSeq.create(wordcs);
    word = CharSeq.intern(word);

    LemmaInfo lemma = null;
    List<WordInfo> parse = lemmer.parse(word);
    if (parse.size() > 0) {
      lemma = parse.get(0).lemma();
    }

    long wordId = parser.addToken(CharSeq.create(wordcs)).id();
    if (lemma == null) {
      return new ParsedTerm(wordId, word, -1, null, null);
    }

    long lemmaId = parser.addToken(lemma.lemma() + LEMMA_SUFFIX).id();
    return new ParsedTerm(wordId, word, lemmaId, lemma.lemma(),
        PartOfSpeech.valueOf(lemma.pos().name()));
  }

  public CharSeq lemma() {
    return lemma;
  }

  public long lemmaId() {
    return lemmaId;
  }

  public CharSeq word() {
    return word;
  }

  public long wordId() {
    return wordId;
  }

  @Nullable
  public PartOfSpeech posTag() {
    return posTag;
  }

  public boolean hasPosTag() {
    return posTag != null;
  }

  public boolean hasLemma() {
    return lemmaId != -1;
  }
}
