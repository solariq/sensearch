package com.expleague.sensearch.filter;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.core.Annotations.FilterMaxItems;
import com.expleague.sensearch.index.Embedding;
import com.expleague.sensearch.index.plain.Candidate;
import com.google.inject.Inject;

import java.util.function.LongPredicate;
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
    LOG.info("Filtering started");
    long start = System.currentTimeMillis();
    Stream<Candidate> result = embedding.nearest(qVec, predicate);
    long end = System.currentTimeMillis();
    LOG.info(String.format("Filtering finished in %.3f seconds", (end - start) / 1e3));
    return result;
  }

  @Override
  public Stream<Candidate> filtrate(@NotNull Vec qVec, LongPredicate predicate, int numOfNeighbors) {
    LOG.info("Filtering started");
    long start = System.currentTimeMillis();
    Stream<Candidate> result = embedding.nearest(qVec, predicate, numOfNeighbors);
    long end = System.currentTimeMillis();
    LOG.info(String.format("Filtering finished in %.3f seconds", (end - start) / 1e3));
    return result;
  }

  @Override
  public Stream<Candidate> filtrate(@NotNull Vec qVec, LongPredicate predicate, double maxDist) {
    LOG.info("Filtering started");
    long start = System.currentTimeMillis();
    Stream<Candidate> result = embedding.nearest(qVec, predicate, maxDist);
    long end = System.currentTimeMillis();
    LOG.info(String.format("Filtering finished in %.3f seconds", (end - start) / 1e3));
    return result;
  }
}
