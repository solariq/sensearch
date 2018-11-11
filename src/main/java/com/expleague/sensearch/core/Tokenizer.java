package com.expleague.sensearch.core;

import java.util.regex.Pattern;

/**
 * Created by sandulmv on 11.11.18.
 */
public class Tokenizer {
  private static final Pattern REGEXP_SPLITTER = Pattern.compile("[^А-ЯЁа-яёA-Za-z0-9]");
  private Tokenizer(){}

  public static String[] tokenize(CharSequence charSequence) {
    return REGEXP_SPLITTER.split(charSequence);
  }
}
