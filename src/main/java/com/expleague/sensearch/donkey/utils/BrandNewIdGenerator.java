package com.expleague.sensearch.donkey.utils;

import static com.expleague.sensearch.core.IdUtils.BITS_FOR_FEATURES;

import com.expleague.commons.seq.CharSeq;
import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.net.URI;

/**
 * НЕ ТРЭД-СЭЙФ (C)Ментор
 */
public class BrandNewIdGenerator {

  private static final BrandNewIdGenerator instance = new BrandNewIdGenerator();

  private static final int DEFAULT_SEED = 42;
  private static final HashFunction MURMUR = Hashing.murmur3_128(DEFAULT_SEED);

  private TObjectIntMap<CharSeq> termToIntMap = new TObjectIntHashMap<>();
  private TIntObjectMap<CharSeq> intToTermMap = new TIntObjectHashMap<>();

  public static int ID = 0;
//  public static int META = 0;
  private final static int BITS_FOR_META = 8;
  private final static int FIRST_UPPERCASE = 0x00000008; //0000'0000'0000'0000'0000'0000'0000'1000
  private final static int ALL_UPPERCASE = 0x00000004;   //0000'0000'0000'0000'0000'0000'0000'0100
  private final static int PUNCTUATION = 0x00000002;     //0000'0000'0000'0000'0000'0000'0000'0010

  private BrandNewIdGenerator() {}

  public long generatePageId(URI uri) {
    long hash = MURMUR.hashString(uri.toString(), Charsets.UTF_8).asInt();
    if (hash > 0) {
      hash = -hash;
    }
    return hash << BITS_FOR_FEATURES;
  }

  public long generateTermId(CharSequence term) {
    long hash = MURMUR.hashString(term, Charsets.UTF_8).asLong();
    return hash <= 0 ? (hash == Long.MIN_VALUE ? -(hash + 1) : hash < 0 ? -hash : 1) : hash;
  }

  public static BrandNewIdGenerator getInstance() {
    return instance;
  }

  public TObjectIntMap<CharSeq> termToIntMapping() {
    return termToIntMap;
  }

  public TIntObjectMap<CharSeq> intToTermMapping() {
    return intToTermMap;
  }


  public CharSeq token(int id) {
    CharSeq s = intToTermMap.get(id >>> 4);
    if ((id & ALL_UPPERCASE) > 0) {
      s = CharSeq.intern(s.toString().toUpperCase());
    } else if ((id & FIRST_UPPERCASE) > 0) {
      String res = Character.toUpperCase(s.charAt(0)) + s.subSequence(1).toString();
      s = CharSeq.intern(res);
    }
    return s;
  }


  public int addToken(CharSeq token) throws Exception {
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
      intToTermMap.put(id, lowToken);
      ID++;
//      if (punkt) META++;
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
    return id;
  }
}
