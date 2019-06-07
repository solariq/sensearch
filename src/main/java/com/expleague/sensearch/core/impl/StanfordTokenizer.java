package com.expleague.sensearch.core.impl;

import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.core.Tokenizer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.io.input.CharSequenceReader;

public class StanfordTokenizer implements Tokenizer {

  private static TokenizerFactory<CoreLabel> PT = PTBTokenizer
      .factory(new CoreLabelTokenFactory(), "ptb3Escaping=false, invertible=true, untokenizable=noneKeep");

  public StanfordTokenizer() {
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
    DocumentPreprocessor dp = new DocumentPreprocessor(new CharSequenceReader(text));
    dp.setTokenizerFactory(PT);
    return StreamSupport.stream(dp.spliterator(), false).map(SentenceUtils::listToOriginalTextString);
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
