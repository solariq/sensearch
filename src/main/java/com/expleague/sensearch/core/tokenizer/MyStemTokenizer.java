package com.expleague.sensearch.core.tokenizer;

import com.expleague.commons.text.lemmer.MyStem;
import com.expleague.commons.text.lemmer.MyStemImpl;
import com.expleague.commons.text.lemmer.WordInfo;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MyStemTokenizer implements Tokenizer {

  private static final Pattern splitPattern = Pattern
      .compile("(?<=[.!?]|[.!?]['\"])(?=\\p{javaWhitespace}*\\p{javaUpperCase})");

  private MyStem myStem;

  public MyStemTokenizer(Path myStemPath) {
    this.myStem = new MyStemImpl(myStemPath);
  }

  @Override
  public List<CharSequence> toWords(CharSequence sentence) {
    return myStem
        .parse(sentence)
        .stream()
        .map(WordInfo::token)
        .collect(Collectors.toList());
  }

  @Override
  public List<CharSequence> toSentences(CharSequence text) {
    return Arrays.asList(splitPattern.split(text));
  }

}
