package com.expleague.sensearch.core.lemmer;

import com.expleague.commons.seq.CharSeqTools;
import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.MyStemImpl;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;


public class Lemmer implements MyStem {
  private final RussianLemmer russianLemmer;
  private final EnglishLemmer englishLemmer;
//  private static final Lemmer LEMMER = new Lemmer();
  private Tokenizer tokenizer = new TokenizerImpl();


  public Lemmer() {
    this(new RussianLemmer(),  new EnglishLemmer());
  }

  public Lemmer(RussianLemmer russianLemmer, EnglishLemmer englishLemmer) {
    this.russianLemmer = russianLemmer;
    this.englishLemmer = englishLemmer;
  }

  public static Lemmer getInstance() {
    return new Lemmer();
  }

  @Override
  public List<WordInfo> parse(CharSequence seq) {
    return tokenizer
        .toWords(seq)
        .map(this::parseWord)
        .collect(Collectors.toList());
  }

  private WordInfo parseWord(final CharSequence forParse) {
    final CharSequence word = CharSeqTools.toLowerCase(forParse);
    final char firstLetter = word.length() > 0 ? word.charAt(0) : 'a';
    if (firstLetter >= (int) 'a' && firstLetter <= (int) 'z' ||
        firstLetter >= (int) 'A' && firstLetter <= (int) 'Z'){
      return englishLemmer.parse(word);
    }
    else {
      return russianLemmer.parse(word);
    }
  }
}
