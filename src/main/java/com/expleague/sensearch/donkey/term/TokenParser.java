package com.expleague.sensearch.donkey.term;

import static com.expleague.sensearch.core.TokenIdUtils.BITS_FOR_META;
import static com.expleague.sensearch.core.TokenIdUtils.PUNCTUATION_SIZE;
import static com.expleague.sensearch.core.TokenIdUtils.allUpperCase;
import static com.expleague.sensearch.core.TokenIdUtils.firstUpperCase;
import static com.expleague.sensearch.core.TokenIdUtils.setAllUpperCase;
import static com.expleague.sensearch.core.TokenIdUtils.setFirstUpperCase;
import static com.expleague.sensearch.core.TokenIdUtils.setPunctuation;
import static com.expleague.sensearch.core.TokenIdUtils.toId;

import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.core.lemmer.Lemmer;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TokenParser implements AutoCloseable {

  private static final char LEMMA_SUFFIX = '$';

  private int curPunctId = 0;
  private int curTermId = PUNCTUATION_SIZE;

  private final Dictionary dictionary;
  private final Tokenizer tokenizer;
  private final Lemmer lemmer;
  private final TIntObjectMap<Token> tokenCache = new TIntObjectHashMap<>();

  public TokenParser(Dictionary dictionary, Lemmer lemmer, Tokenizer tokenizer) {
    this.dictionary = dictionary;
    this.lemmer = lemmer;
    this.tokenizer = tokenizer;
  }

  public void check(CharSequence originalText, int[] ids) {
    boolean check = true;
    boolean upper;

    StringBuilder res = new StringBuilder();

    int id = 0;
    CharSequence w = "";
    if (id < ids.length) {
      w = formattedText(ids[id]);
    }
    int j = 0;
    for (int i = 0; i < originalText.length(); i++) {
      upper = allUpperCase(ids[id]);
      if (w.charAt(j) != originalText.charAt(i)) {
        if (upper || j == 0 || w.charAt(j) != Character.toLowerCase(originalText.charAt(i))) {
          check = false;
          break;
        }
      }
      j++;
      if (j == w.length()) {
        res.append(w);
        j = 0;
        id++;
        if (id == ids.length && i != originalText.length() - 1) {
          check = false;
          break;
        }
        if (id < ids.length) {
          w = formattedText(ids[id]);
        }
      }
    }
    if (j != 0 || id != ids.length) {
      check = false;
    }

    if (!check) {
      throw new RuntimeException("Parsed text::\n" + res + "\nAren't equal original text::\n" + originalText);
    }
  }

  private CharSequence formattedText(int id) {
    CharSequence t = dictionary.get(toId(id));
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
    tokenizer.toWords(text).forEach(t -> {
      result.add(addToken(t));
    });
    return result.stream();
  }

  public Token addToken(CharSequence token) {
    if (token.length() == 0) {
      throw new IllegalArgumentException("Empty token encountered");
    }
    return addToken(CharSeq.intern(token));
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
    if (dictionary.contains(lowToken)) {
      id = dictionary.get(lowToken);
    } else {
      if (punkt) {
        id = curPunctId;
        curPunctId++;
      } else {
        id = curTermId;
        curTermId++;
      }
      if (curTermId >= (1 << 29)) {
        throw new RuntimeException("Token limit::" + token.toString());
      }
      if (curPunctId == PUNCTUATION_SIZE) {
        throw new RuntimeException("Punctuation limit::" + token.toString());
      }
    }
    id = id << BITS_FOR_META;
    if (punkt) {
      id = setPunctuation(id);
    } else {
      if (firstUp) {
        id = setFirstUpperCase(id);
      }
      if (allUp[0]) {
        id = setAllUpperCase(id);
      }
    }
    if (tokenCache.containsKey(id)) {
      return tokenCache.get(id);
    } else {
      Token result = punkt ? createPunct(lowToken, id) : createTerm(lowToken, id);
      tokenCache.put(id, result);
      return result;
    }
  }

  private Token createPunct(CharSeq lowToken, int id) {
    Token token = new Token(lowToken, id);
    dictionary.addTerm(new ParsedTerm(token));
    return token;
  }

  private Token createTerm(CharSeq lowText, int id) {
    Token token = new Token(lowText, id);
    CharSeq word = CharSeq.intern(token.text());
    WordInfo parse = lemmer.parse(word);
    LemmaInfo lemma = parse.lemma();

    int wordId = token.id();
    if (lemma == null) {
      dictionary.addTerm(new ParsedTerm(token));
      return token;
    }

    Token lemmaToken;
    if (dictionary.contains(lemma.lemma())) {
      lemmaToken = tokenCache.get(dictionary.get(lemma.lemma()));
    } else if (token.text().charAt(token.text().length() - 1) == LEMMA_SUFFIX) {
      lemmaToken = token;
    } else {
      lemmaToken = addToken(lemma.lemma() + String.valueOf(LEMMA_SUFFIX));
    }
    dictionary.addTerm(new ParsedTerm(token, lemmaToken, PartOfSpeech.valueOf(lemma.pos().name())));

    return token;
  }

  @Override
  public void close() {
    dictionary.close();
  }

}
