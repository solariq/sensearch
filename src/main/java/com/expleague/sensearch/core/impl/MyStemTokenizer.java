package com.expleague.sensearch.core.impl;

import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.MyStemImpl;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.core.Tokenizer;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.of;

public class MyStemTokenizer implements Tokenizer {
    private static final Pattern splitPattern =
      Pattern.compile("(?<=[.!?]|[.!?]['\"])(?=\\p{javaWhitespace}*\\p{javaUpperCase})");
//  private static final Pattern splitPattern = Pattern.compile("[^А-ЯЁа-яёA-Za-z0-9]");

  private MyStem myStem;

  public MyStemTokenizer(Path myStemPath) {
    this.myStem = new MyStemImpl(myStemPath);
  }

  @Override
  public Stream<CharSequence> toWords(CharSequence text) {
    return myStem.parse(text).stream().map(WordInfo::token);
  }

  @Override
  public Stream<CharSequence> toSentences(CharSequence text) {
    return Stream.of(splitPattern.split(text));
  }

  private final Pattern REGEXP_SPLITTER = Pattern.compile("[^А-ЯЁа-яёA-Za-z0-9]");
  @Override
  public Stream<CharSequence> parse(CharSequence text) {
    return Stream.<CharSequence>of(REGEXP_SPLITTER.split(text)).filter(s -> s.length() != 0);
  }
}
