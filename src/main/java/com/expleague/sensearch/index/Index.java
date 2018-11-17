package com.expleague.sensearch.index;

import com.expleague.sensearch.query.term.Term;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.query.Query;
import java.util.stream.Stream;

public interface Index {

  Stream<Page> fetchDocuments(Query query);

  Term[] synonyms(Term term);

  boolean isPage(long id);

  boolean isWord(long id);

  int indexSize();

  int vocabularySize();
  double averagePageSize();

  int documentFrequency(Term term);
  long termFrequency(Term term);
}
