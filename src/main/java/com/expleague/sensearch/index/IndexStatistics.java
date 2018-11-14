package com.expleague.sensearch.index;

import com.expleague.sensearch.query.term.Term;

/**
 * Created by sandulmv on 02.11.18.
 */
public interface IndexStatistics {
  int indexSize();

  int vocabularySize();
  double averagePageSize();

  int documentFrequency(Term term);
  long termFrequency(Term term);
}
