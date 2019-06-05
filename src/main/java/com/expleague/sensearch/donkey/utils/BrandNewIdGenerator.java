package com.expleague.sensearch.donkey.utils;

import static com.expleague.sensearch.core.IdUtils.BITS_FOR_FEATURES;

import com.expleague.commons.seq.CharSeq;
import com.expleague.sensearch.Page;
import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.net.URI;

public class BrandNewIdGenerator {

  private static BrandNewIdGenerator instance;

  static {
    instance = new BrandNewIdGenerator();
  }

  private static final int DEFAULT_SEED = 42;
  private static final HashFunction MURMUR = Hashing.murmur3_128(DEFAULT_SEED);

  private TObjectIntMap<CharSeq> termToIntMap = new TObjectIntHashMap<>();
  private TIntObjectMap<CharSeq> intToTermMap = new TIntObjectHashMap<>();
  private final PageParser parser = new PageParser();

  private static int ID = 0;
  public final static CharSeq END_OF_PARAGRAPH = CharSeq.create("777@END@777");
  private final int END_ID;
  public final static CharSeq END_OF_TITLE = CharSeq.create(Page.TITLE_DELIMETER);
  private final int END_TITLE;
  public final static int FIRST_UPPERCASE = 0x00000008;
  public final static int ALL_UPPERCASE = 0x00000004;

  private BrandNewIdGenerator() {
    END_ID = addToken(END_OF_PARAGRAPH) >>> 4;
    END_TITLE = addToken(END_OF_TITLE) >>> 4;
  }

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


  private int addToken(CharSeq token) {
    boolean firstUp = false;
    final boolean[] allUp = {true};
    int id;
    if (Character.isUpperCase(token.at(0))) {
      firstUp = true;
    }
    token.forEach(c -> {
      if (Character.isLowerCase(c)) {
        allUp[0] = false;
      }
    });
    CharSeq lowToken = CharSeq.create(token.toString().toLowerCase());
    if (termToIntMap.containsKey(lowToken)) {
      id = termToIntMap.get(lowToken);
    } else {
      id = ID;
      termToIntMap.put(lowToken, id);
      intToTermMap.put(id, lowToken);
      ID++;
    }
    id = id << 8;
    if (firstUp) {
      id |= FIRST_UPPERCASE;
    }
    if (allUp[0]) {
      id |= ALL_UPPERCASE;
    }
    return id;
  }
}
