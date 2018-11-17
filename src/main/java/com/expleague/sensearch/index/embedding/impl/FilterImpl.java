package com.expleague.sensearch.index.embedding.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.core.Embedding;
import com.expleague.sensearch.core.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;

public class FilterImpl implements Filter {

  private static final int START_MULTIPLIER = 2;
  private static final int MAX_NUMBER = 2_000_000;

  private Embedding embedding;

  public FilterImpl(Embedding embedding) {
    this.embedding = embedding;
  }

  @Override
  public LongStream filtrate(Vec mainVec, int number, LongPredicate predicate) {
    int embNumber = number * START_MULTIPLIER;
    List<Long> result = new ArrayList<>();
    while (embNumber < MAX_NUMBER) {
      result.clear();
      embedding.getNearest(mainVec, number).filter(predicate).forEach(result::add);
      if (result.size() >= number) {
        return result.subList(0, number).stream().mapToLong(Long::longValue);
      }
      embNumber += number;
    }
    throw new IllegalArgumentException("number is too large");
  }
}