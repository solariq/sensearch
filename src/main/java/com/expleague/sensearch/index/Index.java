package com.expleague.sensearch.index;

import com.expleague.sensearch.query.term.Term;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.query.Query;
import java.util.List;
import java.util.stream.Stream;

public interface Index {

  Stream<Page> fetchDocuments(Query query);

  Term[] synonyms(Term term);

  List<String> mostFrequentNeighbours(String rawWord);

  boolean hasTitle(CharSequence title);

  int size();

  int vocabularySize();
  double averagePageSize();

  int documentFrequency(Term term);
  long termFrequency(Term term);
}
