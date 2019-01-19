package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.index.Embedding;
import com.expleague.sensearch.index.Filter;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import java.util.Arrays;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;
import org.apache.log4j.Logger;

public class FilterImpl implements Filter {

  private static final Logger LOG = Logger.getLogger(FilterImpl.class.getName());

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
    long startTime = System.nanoTime();
    LOG.info("Filtering started");

    int embNumber = number * START_MULTIPLIER;
    TLongList result = new TLongArrayList();
    while (embNumber < MAX_NUMBER) {
      result.clear();
      embedding.nearest(mainVec, embNumber, predicate).forEach(result::add);
      if (result.size() >= number) {
        LOG.info(
            String.format(
                "Filtering finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
        break;
      }
      embNumber *= 2;
    }
    return Arrays.stream(result.subList(0, Math.min(number, result.size())).toArray());
  }

  public LongStream filtrate(Vec mainVec, double maxDistance, LongPredicate predicate) {
    return embedding.nearest(mainVec, maxDistance, predicate);
  }
}
