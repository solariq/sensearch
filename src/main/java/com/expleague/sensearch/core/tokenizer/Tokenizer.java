package com.expleague.sensearch.core.tokenizer;

import java.util.List;

public interface Tokenizer {
  List<CharSequence> sentenceToWords(CharSequence sentence);
  List<CharSequence> textToSentences(CharSequence text);
}
