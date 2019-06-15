package com.expleague.sensearch.donkey.utils;

import com.expleague.commons.seq.CharSeq;
import java.util.ArrayList;
import java.util.List;

public class PageParser {

  List<CharSeq> parse(CharSeq text) {
    List<CharSeq> result  = new ArrayList<>();
    StringBuilder token = new StringBuilder();
    final boolean[] isProcessingWord = {false};
    text.forEach(c -> {
      if (Character.isAlphabetic(c) || Character.isDigit(c)) {
        isProcessingWord[0] = true;
        token.append(c);
      } else {
        if (isProcessingWord[0]) {
          result.add(CharSeq.intern(token.toString()));
          token.delete(0, token.length());
          isProcessingWord[0] = false;
        }
        result.add(CharSeq.intern(c.toString()));
      }
    });
    if (token.length() != 0) {
      result.add(CharSeq.intern(token.toString()));
    }

    return result;
  }
}
