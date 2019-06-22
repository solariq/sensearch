package com.expleague.sensearch.core;

import java.util.List;
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
   * Splits content to sentences
   * @param text Text to be splitted
   * @return Sentences of the content
   */
  Stream<CharSequence> toSentences(CharSequence text);

  /**
   * Splits content to sentences
   * @return Sentences of the content
   */
  Stream<List<Term>> toSentences(List<Integer> intText, List<Term> termText);

  /**
   * Splits text to paragraphs.
   *
   * @param text Text to be splitted
   * @return Sentences of the content
   */
  Stream<CharSequence> toParagraphs(CharSequence text);
  /**
   * Splits content into words, removing all punctuation and special characters
   *
   * @param text Text to be split
   * @return Words of this content
   */
  default Stream<CharSequence> parseTextToWords(CharSequence text) {
    return toSentences(text).filter(s -> s.length() > 0).flatMap(this::toWords);
  }
}
