package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.index.Embedding;
import com.expleague.sensearch.index.Filter;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;

import java.util.ArrayList;
import java.util.Arrays;
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
  // TODO: move filtrate method to Index and delete FilterImpl?
  // NO, GOD, PLS NO!!
  public LongStream filtrate(Vec mainVec, int number, LongPredicate predicate) {
    int embNumber = number * START_MULTIPLIER;
    TLongList result = new TLongArrayList();
    while (embNumber < MAX_NUMBER) {
      result.clear();
      embedding.getNearest(mainVec, embNumber).filter(predicate).forEach(result::add);
      if (result.size() >= number) {
        return Arrays.stream(result.subList(0, number).toArray());
      }
      embNumber *= 2;
    }
    throw new IllegalArgumentException("number is too large");
  }
}
