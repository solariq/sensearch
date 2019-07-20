package com.expleague.sensearch.index;


import com.expleague.sensearch.core.Term;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;

/**
 * FIXME: Methods accept objects of the type 'Term' BUT to get statistics we will need term id which
 * we cannot have from such an object. This is true for all methods of the interface
 * Probably, methods should accept some other type which can return term id
 **/
public interface TermStatisticsBase {

  /**
   * @return count of occurrences of a term in all documents of the index
   */
  long termFrequency(Term term);

  /**
   * @return count of documents in which given term occurs
   */
  int documentFrequency(Term term);

  /**
   * @return stream of pairs of term and and it frequency of how often it occurs with the given term
   */
  Stream<Pair<Term, Integer>> mostFrequentNeighbours(Term term, int neighCount);

  void saveTo(Path path);
}
