package com.expleague.sensearch.core;

import com.expleague.sensearch.core.impl.MyStemTokenizer;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public interface Tokenizer {
  Stream<CharSequence> toWords(CharSequence sentence);
  Stream<CharSequence> toSentences(CharSequence text);

  Stream<CharSequence> parse(CharSequence text);
}
