package com.expleague.sensearch.index;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public interface Index {

  @Nullable
  Page page(URI uri);

  Stream<Page> fetchDocuments(Query query);

  Term[] synonyms(Term term);

  List<String> mostFrequentNeighbours(String rawWord);

  boolean hasTitle(CharSequence title);

  int size();

  int vocabularySize();

  double averagePageSize();

  int documentFrequency(Term term);

  long termFrequency(Term term);

  Tokenizer tokenizer();
}
