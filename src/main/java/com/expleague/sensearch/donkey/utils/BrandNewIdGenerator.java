package com.expleague.sensearch.donkey.utils;

import static com.expleague.sensearch.donkey.plain.IdUtils.BITS_FOR_FEATURES;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import gnu.trove.set.TLongSet;
import java.net.URI;

public class BrandNewIdGenerator {
  private static final int DEFAULT_SEED = 42;
  private static final HashFunction MURMUR = Hashing.murmur3_128(DEFAULT_SEED);

  private BrandNewIdGenerator() {}

  public static GenericIdGenerator pageIdGenerator(URI uri) {
    long hash = MURMUR.hashString(uri.toString(), Charsets.UTF_8).asInt();
    if (hash > 0) {
      hash = -hash;
    }

    return new GenericIdGenerator(hash << BITS_FOR_FEATURES, 1L << BITS_FOR_FEATURES);
  }

  public static GenericIdGenerator termIdGenerator(CharSequence term) {
    long hash = MURMUR.hashString(term, Charsets.UTF_8).asLong();
    hash = hash <= 0 ? (hash == Long.MIN_VALUE ? -(hash + 1) : hash < 0 ? -hash : 1) : hash;
    return new GenericIdGenerator(hash, -1);
  }

  public static class GenericIdGenerator {
    private final long seed;
    private final long increment;
    private int iteration;

    GenericIdGenerator(long seed, long increment) {
      this.seed = seed;
      this.increment = increment;
      this.iteration = 0;
    }

    // TODO: check if hash whithin bound
    public long next() {
      return seed + increment * iteration++;
    }

    public long next(TLongSet forbiddenIds) {
      long id;
      while (forbiddenIds.contains((id = next()))) {}
      return id;
    }

    public void reset() {
      iteration = 0;
    }
  }
}
