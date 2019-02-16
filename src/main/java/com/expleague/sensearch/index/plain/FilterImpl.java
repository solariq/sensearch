package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.core.Annotations.FilterMaxItems;
import com.expleague.sensearch.index.Embedding;
import com.expleague.sensearch.index.Filter;
import com.google.inject.Inject;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import java.util.Arrays;
import java.util.function.LongPredicate;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class FilterImpl implements Filter {

  private static final Logger LOG = Logger.getLogger(FilterImpl.class.getName());

  private static final int START_MULTIPLIER = 2;

  private final Embedding embedding;
  private final int maxItems;

  @Inject
  public FilterImpl(Embedding embedding, @FilterMaxItems int maxItems) {
    this.embedding = embedding;
    this.maxItems = maxItems;
  }

  @Override
  public Stream<Candidate> filtrate(@NotNull Vec qVec, LongPredicate predicate) {
    return embedding.nearest(qVec, predicate);
  }

  @Override
  // TODO: uncomment and fix
  public Stream<Candidate> filtrate(@NotNull Vec qVec, int number, LongPredicate predicate) {
    /*long startTime = System.nanoTime();
    LOG.info("Filtering started");

    int embNumber = number * START_MULTIPLIER;
    TLongList result = new TLongArrayList();
    while (embNumber < maxItems) {
      result.clear();
      embedding.nearest(qVec, embNumber, predicate).forEach(result::add);
      if (result.size() >= number) {
        LOG.info(
            String.format(
                "Filtering finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
        break;
      }
      embNumber *= 2;
    }
    return Arrays.stream(result.subList(0, Math.min(number, result.size())).toArray());*/
    return embedding.nearest(qVec, number, predicate);
  }

  @Override
  public Stream<Candidate> filtrate(@NotNull Vec qVec, double maxDistance, LongPredicate predicate) {
    return embedding.nearest(qVec, maxDistance, predicate);
  }
}
