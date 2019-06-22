package com.expleague.sensearch.core.impl;

import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.core.Tokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class TokenizerImpl implements Tokenizer {

  public TokenizerImpl() {
  }

  @Override
  public Stream<CharSequence> toWords(CharSequence sentence) {
    List<CharSequence> words = new ArrayList<>();
    int wordStart = -1;
    int length = sentence.length();
    for (int i = 0; i < length; i++) {
      char c = sentence.charAt(i);
      if (wordStart == -1 && (Character.isDigit(c) || isAlpha(c))) {
        wordStart = i;
      }
      if (wordStart != -1 && !Character.isDigit(c) && !isAlpha(c)) {
        words.add(sentence.subSequence(wordStart, i));
        wordStart = -1;
      }
    }

    if (wordStart != -1) {
      words.add(sentence.subSequence(wordStart, length));
    }

    return words.stream();
  }

  @Override
  public Stream<CharSequence> toParagraphs(CharSequence text) {
    return Arrays.stream(CharSeqTools.split(text, "\n")).map(CharSeqTools::trim);
  }

  @Override
  public Stream<CharSequence> toSentences(CharSequence text) {
    int endOfSentencePos = -1;
    int sentenceStart = 0;
    List<CharSequence> sentences = new ArrayList<>();

    int length = text.length();
    for (int i = 0; i < length; i++) {
      char c = text.charAt(i);
      if (c == '!' || c == '?' || c == '.') {
        endOfSentencePos = i;
      }
      if (endOfSentencePos != -1 && endOfSentencePos == i - 1 && (c == '\'' || c == '"')) {
        endOfSentencePos = i;
      }
      if (Character.isWhitespace(c)) {
        continue;
      }
      boolean isUpperCase = isUpperCase(c);

      if (endOfSentencePos != -1 && isUpperCase) {
        if (sentenceStart == 0) {
          sentenceStart = trimStart(sentenceStart, text);
        }
        sentences.add(text.subSequence(sentenceStart, endOfSentencePos + 1));
        endOfSentencePos = -1;
        sentenceStart = i;
      }

      if (endOfSentencePos != -1
          && (c == '\'' || c == '"')
          && i + 1 < text.length()
          && isUpperCase(text.charAt(i + 1))) {

        if (sentenceStart == 0) {
          sentenceStart = trimStart(sentenceStart, text);
        }

        sentences.add(text.subSequence(sentenceStart, endOfSentencePos + 1));
        endOfSentencePos = -1;
        sentenceStart = i;
      }
    }

    sentences.add(CharSeqTools.trim(text.subSequence(sentenceStart, text.length())));

    return sentences.stream().filter(s -> s.length() > 0);
  }

  @Override
  public Stream<List<Term>> toSentences(List<Integer> intText, List<Term> termText) {
    if (intText.size() != termText.size()) {
      return Stream.empty();
    }
    int sentenceStart = 0;
    List<List<Term>> sentences = new ArrayList<>();
    int length = intText.size();
    for (int i = 0; i < length; i++) {
      CharSequence c = termText.get(i).text();
      if (c == "!" || c == "?" || c == ".") {
        sentences.add(termText.subList(sentenceStart, i + 1));
        sentenceStart = i + 1;
      }
    }
    if (sentenceStart != length) {
      sentences.add(termText.subList(sentenceStart, length));
    }



//    for (int i = 0; i < length; i++) {
//      String c = String.valueOf(termText.get(i).text());
//      if (c.equals("!") || c.equals("?") || c.equals(".")) {
//        endOfSentencePos = i;
//      }
//      if (endOfSentencePos != -1 && endOfSentencePos == i - 1 && (c.equals("'") || c.equals("\""))) {
//        endOfSentencePos = i;
//      }
//      if (Character.isWhitespace(c.charAt(0))) {
//        continue;
//      }
//      boolean isUpperCase = isUpperCase(c.charAt(0));
//
//      if (endOfSentencePos != -1 && isUpperCase) {
//        sentences.add(termText.subList(sentenceStart, endOfSentencePos + 1));
//        endOfSentencePos = -1;
//        sentenceStart = i;
//      }
//
//      if (endOfSentencePos != -1
//          && (c.equals("'") || c.equals("\""))
//          && i + 1 < intText.size()
//          && isUpperCase(text.charAt(i + 1))) {
//
//        sentences.add(termText.subList(sentenceStart, endOfSentencePos + 1));
//        endOfSentencePos = -1;
//        sentenceStart = i;
//      }
//    }

    return sentences.stream().filter(s -> s.size() > 1);
  }

  private int trimStart(int sentenceStart, CharSequence text) {
    while (sentenceStart < text.length() && Character.isWhitespace(text.charAt(sentenceStart))) {
      sentenceStart++;
    }
    return sentenceStart;
  }

  private boolean isUpperCase(char c) {
    return Character.isUpperCase(c);
  }

  private boolean isAlpha(char c) {
    return Character.isLetter(c);
  }
}
