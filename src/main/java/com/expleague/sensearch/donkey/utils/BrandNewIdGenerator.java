package com.expleague.sensearch.donkey.utils;

import static com.expleague.sensearch.core.IdUtils.BITS_FOR_FEATURES;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.net.URI;

public class BrandNewIdGenerator {
  private static final int DEFAULT_SEED = 42;
  private static final HashFunction MURMUR = Hashing.murmur3_128(DEFAULT_SEED);

  private BrandNewIdGenerator() {}

  public static long generatePageId(URI uri) {
    long hash = MURMUR.hashString(uri.toString(), Charsets.UTF_8).asInt();
    if (hash > 0) {
      hash = -hash;
    }
    return hash << BITS_FOR_FEATURES;
  }

  public static long generateTermId(CharSequence term) {
    long hash = MURMUR.hashString(term, Charsets.UTF_8).asLong();
    return hash <= 0 ? (hash == Long.MIN_VALUE ? -(hash + 1) : hash < 0 ? -hash : 1) : hash;
  }

}
