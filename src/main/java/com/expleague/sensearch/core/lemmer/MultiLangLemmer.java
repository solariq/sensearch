package com.expleague.sensearch.core.lemmer;

import com.expleague.commons.seq.CharSeqTools;
import com.expleague.commons.text.lemmer.WordInfo;


public class MultiLangLemmer implements Lemmer {

  private final RussianLemmer russianLemmer;
  private final EnglishLemmer englishLemmer;

  public MultiLangLemmer() {
    this(new RussianLemmer(), new EnglishLemmer());
  }

  public MultiLangLemmer(RussianLemmer russianLemmer, EnglishLemmer englishLemmer) {
    this.russianLemmer = russianLemmer;
    this.englishLemmer = englishLemmer;
  }

  public static Lemmer getInstance() {
    return new MultiLangLemmer();
  }

  @Override
  public WordInfo parse(CharSequence seq) {
    final CharSequence word = CharSeqTools.toLowerCase(seq);
    final char firstLetter = word.length() > 0 ? word.charAt(0) : 'a';
    if (firstLetter >= (int) 'a' && firstLetter <= (int) 'z' ||
        firstLetter >= (int) 'A' && firstLetter <= (int) 'Z') {
      return englishLemmer.parse(word);
    } else {
      return russianLemmer.parse(word);
    }
  }
}
