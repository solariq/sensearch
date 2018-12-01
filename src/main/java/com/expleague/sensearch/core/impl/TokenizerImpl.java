package com.expleague.sensearch.core.impl;

import com.expleague.sensearch.core.Tokenizer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TokenizerImpl implements Tokenizer {

  private static final Pattern SENTENCE_SPLIT_PATTEN =
      Pattern.compile("(?<=[.!?]|[.!?]['\"])(?=\\p{javaWhitespace}*\\p{javaUpperCase})");
  private static final Pattern WORD_SPLIT_PATTERN = Pattern.compile("[^А-ЯЁа-яёA-Za-z0-9]");

  public TokenizerImpl() {

  }

  @Override
  public Stream<CharSequence> toWords(CharSequence sentence) {
    return Stream.<CharSequence>of(WORD_SPLIT_PATTERN.split(sentence)).filter(s -> s.length() > 0);
  }

  @Override
  public Stream<CharSequence> toSentences(CharSequence text) {
    return Stream.<CharSequence>of(SENTENCE_SPLIT_PATTEN.split(text)).filter(s -> s.length() > 0);
  }

}
