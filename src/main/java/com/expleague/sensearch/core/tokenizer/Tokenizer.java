package com.expleague.sensearch.core.tokenizer;

import java.util.List;

public interface Tokenizer {
  List<CharSequence> toWords(CharSequence sentence);
  List<CharSequence> toSentences(CharSequence text);
}
