package com.expleague.sensearch.donkey.utils;

import com.expleague.commons.seq.CharSeq;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TokenParser {

  private TObjectIntMap<CharSeq> termToIntMap;
  private TIntObjectMap<CharSeq> intToTermMap;

  private static int ID = 5_000;
  private static int PUNCT_ID = 0;
  private static final int PUNCTUATION_SIZE = 5_000;
  private final static int BITS_FOR_META = 8;
  private final static int FIRST_UPPERCASE = 0x00000008; //0000'0000'0000'0000'0000'0000'0000'1000
  private final static int ALL_UPPERCASE = 0x00000004;   //0000'0000'0000'0000'0000'0000'0000'0100
  private final static int PUNCTUATION = 0x00000002;     //0000'0000'0000'0000'0000'0000'0000'0010
  private final PageParser parser = new PageParser();

  public TokenParser() {
    termToIntMap = new TObjectIntHashMap<>();
    intToTermMap = new TIntObjectHashMap<>();
  }

  public TokenParser(TObjectIntMap<CharSeq> map) {
    termToIntMap = map;
    intToTermMap = new TIntObjectHashMap<>();
    map.forEachEntry((cs, i) ->  {
      intToTermMap.put(i, cs);
      return true;
    });
    ID += termToIntMap.size();
  }

  public boolean check(CharSequence originalText, int[] ids) {
    boolean check = true;
    boolean upper;

    int id = 0;
    CharSequence w = formatedText(ids[id]);
    int j = 0;
    for (int i = 0 ; i < originalText.length(); i++) {
      upper = firstUpperCase(ids[id]);
      if (w.charAt(j) != originalText.charAt(i)) {
        if (!upper || j == 0 || Character.toUpperCase(w.charAt(j)) != originalText.charAt(i)) {
          check = false;
          break;
        }
      }
      j++;
      if (j == w.length()) {
        j = 0;
        id++;
        if (id == ids.length && i != originalText.length()) {
          check = false;
          break;
        }
        if (id < ids.length) {
          w = formatedText(ids[id]);
        }
      }
    }
    if (j != 0 || id != ids.length) {
      check = false;
    }

    return check;
  }

  private CharSequence formatedText(int id) {
    CharSequence t = intToTermMap.get(toId(id));
    if (allUpperCase(id)) {
      t = t.toString().toUpperCase();
    } else if (firstUpperCase(id)) {
      CharSequence cp = t.subSequence(1, t.length());
      t = String.valueOf(Character.toUpperCase(t.charAt(0))) + cp;
    }
    return t;
  }

  public Stream<Token> parse(CharSequence text) {
    List<Token> result = new ArrayList<>();
    parser.parse(text).forEach(t -> {
      result.add(addToken(t));
    });
    return result.stream();
  }

  public Token addToken(CharSequence token) {
    return addToken(CharSeq.intern(token));
  }

  public static int toId(int id) {
    return id >>> BITS_FOR_META;
  }

  public static boolean isWord(int id) {
    return (id & PUNCTUATION) == 0;
  }

  /**
   * @param id real id
   * @return true if id in punctuation
   */
  public static boolean isPunct(int id) {
    return id < PUNCTUATION_SIZE;
  }

  public static boolean allUpperCase(int id) {
    return (id & ALL_UPPERCASE) != 0;
  }

  public static boolean firstUpperCase(int id) {
    return (id & FIRST_UPPERCASE) != 0;
  }

  private Token addToken(CharSeq token) {
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
      if (punkt) {
        id = PUNCT_ID;
        PUNCT_ID++;
      } else {
        id = ID;
        ID++;
      }
      termToIntMap.put(lowToken, id);
      intToTermMap.put(id, lowToken);
      if (ID >= (1 << 29)) {
        throw new RuntimeException("Token limit::" + token.toString());
      }
      if (PUNCT_ID == PUNCTUATION_SIZE) {
        throw new RuntimeException("Punctuation limit::" + token.toString());
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
    public long id() {
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
    public CharSequence text() {
      return text;
    }

    public boolean isWord() {
      return (id & PUNCTUATION) == 0;
    }

    public boolean allUpperCase() {
      return (id & ALL_UPPERCASE) != 0;
    }

    public boolean firstUpperCase() {
      return (id & FIRST_UPPERCASE) != 0;
    }
  }
}
