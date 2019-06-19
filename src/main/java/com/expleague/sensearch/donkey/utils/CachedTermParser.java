package com.expleague.sensearch.donkey.utils;

import com.expleague.sensearch.core.lemmer.Lemmer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;

public class CachedTermParser {
  // TODO: make sure this character wil NEVER appear in the word
  private final LoadingCache<CharSequence, ParsedTerm> cache;

  public CachedTermParser(TokenParser parser, Lemmer lemmer, int maxCacheSize) {
    this.cache = CacheBuilder.newBuilder()
        .maximumSize(maxCacheSize)
        .concurrencyLevel(1000)
        .build(CacheLoader.from(cs -> ParsedTerm.parse(cs, lemmer, parser)));
  }

  public ParsedTerm parseTerm(CharSequence word) {
    try {
      return cache.get(word);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}
