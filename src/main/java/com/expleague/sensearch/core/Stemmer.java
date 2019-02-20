package com.expleague.sensearch.core;

import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.core.impl.TokenizerImpl;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Stemmer implements MyStem {

  private com.expleague.commons.text.stem.Stemmer stemmer =
      com.expleague.commons.text.stem.Stemmer.getInstance();
  private Tokenizer tokenizer = new TokenizerImpl();

  @Override
  public List<WordInfo> parse(CharSequence seq) {
    // TODO: partofspeech
    return tokenizer
        .toWords(seq)
        .map(
            word ->
                new WordInfo(
                    CharSeq.create(word),
                    Collections.singletonList(
                        new LemmaInfo(
                            CharSeq.create(doStem(word.toString())),
                            1,
                            com.expleague.commons.text.lemmer.PartOfSpeech.S))))
        .collect(Collectors.toList());
  }

  private String doStem(String word) {
    String newWord = stemmer.stem(word).toString();
    while (!newWord.equals(word)) {
      word = newWord;
      newWord = stemmer.stem(word).toString();
    }
    return newWord;
  }
}
