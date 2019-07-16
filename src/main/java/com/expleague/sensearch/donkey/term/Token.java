package com.expleague.sensearch.donkey.term;

import static com.expleague.sensearch.core.TokenIdUtils.BITS_FOR_META;

import com.expleague.commons.seq.CharSeq;
import com.expleague.sensearch.core.TokenIdUtils;

public class Token {

  private final CharSeq text;
  private final int id;

  public Token(CharSeq text, int id) {
    this.text = text;
    this.id = id;
  }

  /**
   * @return id without META-data
   */
  public int id() {
    return (id >>> BITS_FOR_META);
  }

  /**
   * @return id with META-data
   */
  public int formId() {
    return id;
  }

  /**
   * @return lowercase text
   */
  public CharSequence text() {
    return text;
  }

  public boolean isWord() {
    return TokenIdUtils.isWord(id);
  }

  public boolean allUpperCase() {
    return TokenIdUtils.allUpperCase(id);
  }

  public boolean firstUpperCase() {
    return TokenIdUtils.firstUpperCase(id);
  }
}
