package com.expleague.sensearch.index;


import com.expleague.sensearch.core.Term;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;

public interface TermStatisticsBase {
  long termFrequency(Term term);
  int documentFrequency(Term term);
  Stream<Pair<Term, Integer>> mostFrequentNeighbours(Term term, int neighCount);
}
