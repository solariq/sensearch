package com.expleague.sensearch.index;

import com.expleague.sensearch.query.term.Term;

/**
 * Created by sandulmv on 02.11.18.
 */
public interface IndexStatistics {
  int indexSize();

  int vocabularySize();
  double averageWordsPerPage();

  int pagesWithTerm(Term term);
  long termCollectionFrequency(Term term);
}
