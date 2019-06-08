package com.expleague.sensearch.donkey.utils;

import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.lemmer.Lemmer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;

// Probably temporary class before page preprocessing will be added
public class TermsCache {
  // TODO: make sure this character wil NEVER appear in the word
  private static final String LEMMA_SUFFIX = "$";
  private static final BrandNewIdGenerator ID_GENERATOR = BrandNewIdGenerator.getInstance();
  private final LoadingCache<CharSequence, ParsedTerm> cache;
  private final Lemmer lemmer;
  public TermsCache (Lemmer lemmer, int maxCacheSize) {
    this.lemmer = lemmer;
    this.cache = CacheBuilder.newBuilder()
        .maximumSize(maxCacheSize)
        .concurrencyLevel(1000)
        .build(CacheLoader.from(cs -> ParsedTerm.parse(cs, lemmer)));
  }

  public ParsedTerm parsedTerm(CharSequence word) {
    try {
      return cache.get(word);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public static class ParsedTerm {
    final long wordId;
    final CharSeq word;

    final long lemmaId;
    final CharSeq lemma;

    final PartOfSpeech posTag;

    protected ParsedTerm(long wordId, CharSeq word,
        long lemmaId, CharSeq lemma,
        PartOfSpeech posTag) {
      this.wordId = wordId;
      this.word = word;
      this.lemmaId = lemmaId;
      this.lemma = lemma;
      this.posTag = posTag;
    }

    static ParsedTerm parse(CharSequence wordcs, Lemmer lemmer) {
      CharSeq word = CharSeq.create(wordcs);
      word = CharSeq.intern(word);

      LemmaInfo lemma = null;
      List<WordInfo> parse = lemmer.parse(word);
      if (parse.size() > 0) {
        lemma = parse.get(0).lemma();
      }

      long wordId = ID_GENERATOR.generateTermId(word);
      if (lemma == null) {
        return new ParsedTerm(wordId, word, -1, null, null);
      }

      long lemmaId = ID_GENERATOR.generateTermId(lemma.lemma() + LEMMA_SUFFIX);
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
}
