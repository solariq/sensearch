package com.expleague.sensearch.index;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import java.util.stream.Stream;

public interface Index {

  Stream<Page> fetchDocuments(Query query);

  Stream<String> getSynonyms(String word);

  boolean isPage(long id);

  boolean isWord(long id);

  int indexSize();

  int vocabularySize();
  double averagePageSize();

  int documentFrequency(Term term);
  long termFrequency(Term term);
}
