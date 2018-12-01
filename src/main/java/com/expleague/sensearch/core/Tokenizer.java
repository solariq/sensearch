package com.expleague.sensearch.core;

import java.util.stream.Stream;

public interface Tokenizer {

  /**
   * Splits given sentence to words, removing all punctuation and special characters
   *
   * @param sentence A sentence to be split
   * @return Words of this sentence
   */
  Stream<CharSequence> toWords(CharSequence sentence);

  /**
   * Splits text to sentences
   * @param text Text to be splitted
   * @return Sentences of the text
   */
  Stream<CharSequence> toSentences(CharSequence text);

  /**
   * Splits text into words, removing all punctuation and special characters
   *
   * @param text Text to be split
   * @return Words of this text
   */
  default Stream<CharSequence> parseTextToWords(CharSequence text) {
    return toSentences(text).filter(s -> s.length() > 0).flatMap(this::toWords);
  }
}
