package com.expleague.sensearch.donkey.utils;

import com.expleague.commons.seq.CharSeq;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.ArrayList;
import java.util.List;

public class TokenParser {

  private TObjectIntMap<CharSeq> termToIntMap;

  public static int ID = 0;
  private final static int BITS_FOR_META = 8;
  private final static int FIRST_UPPERCASE = 0x00000008; //0000'0000'0000'0000'0000'0000'0000'1000
  private final static int ALL_UPPERCASE = 0x00000004;   //0000'0000'0000'0000'0000'0000'0000'0100
  private final static int PUNCTUATION = 0x00000002;     //0000'0000'0000'0000'0000'0000'0000'0010
  private final PageParser parser = new PageParser();

  public TokenParser() {
    termToIntMap = new TObjectIntHashMap<>();
  }

  public TokenParser(TObjectIntMap<CharSeq> map) {
    termToIntMap = map;
    ID += termToIntMap.size();
  }

  public List<Token> parseTokens(CharSeq text) {
    List<Token> result = new ArrayList<>();
    parser.parse(text).forEach(t -> {
      try {
        result.add(addToken(t));
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    return result;
  }

  public Token addToken(CharSeq token) throws Exception {
    boolean firstUp = false;
    boolean punkt = true;
    final boolean[] allUp = {true};
    int id;
    if (Character.isUpperCase(token.at(0))) {
      firstUp = true;
    }
    if (Character.isLetterOrDigit(token.at(0))) {
      punkt = false;
    }
    token.forEach(c -> {
      if (Character.isLowerCase(c)) {
        allUp[0] = false;
      }
    });
    CharSeq lowToken = CharSeq.intern(token.toString().toLowerCase());
    if (termToIntMap.containsKey(lowToken)) {
      id = termToIntMap.get(lowToken);
    } else {
      id = ID;
      termToIntMap.put(lowToken, id);
      ID++;
      if (ID >= (1 << 29)) {
        throw new Exception("Token limit::" + token.toString());
      }
    }
    id = id << BITS_FOR_META;
    if (firstUp) {
      id |= FIRST_UPPERCASE;
    }
    if (allUp[0]) {
      id |= ALL_UPPERCASE;
    }
    if (punkt) {
      id |= PUNCTUATION;
    }
    return new Token(token, id);
  }

  public static class Token {

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
     * @return text without changes
     */
    public CharSeq text() {
      return text;
    }

    public boolean isWord() {
      return (id &  PUNCTUATION) == 0;
    }

    public boolean allUpperCase() {
      return (id &  ALL_UPPERCASE) != 0;
    }

    public boolean firstUpperCase() {
      return (id & FIRST_UPPERCASE) != 0;
    }
  }
}
