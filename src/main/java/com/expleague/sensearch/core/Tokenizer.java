package com.expleague.sensearch.core;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Tokenizer {
  private static final Pattern REGEXP_SPLITTER = Pattern.compile("[^А-ЯЁа-яёA-Za-z0-9]");

  private Tokenizer() {
  }

  public static String[] tokenize(CharSequence charSequence) {
    return Stream.of(REGEXP_SPLITTER.split(charSequence))
        .filter(s -> !s.isEmpty())
        .toArray(String[]::new);
  }
}
