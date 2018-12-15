package com.expleague.sensearch.index;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.web.suggest.SuggestInformationLoader;
import com.expleague.sensearch.core.Term;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.stream.Stream;

public interface Index {
  Stream<Page> fetchDocuments(Query query);

  @Nullable
  Term term(CharSequence seq);
  Stream<Term> parse(CharSequence sequence);
  Stream<Term> mostFrequentNeighbours(Term term);

  @Nullable
  Page page(URI reference);

  int size();
  int vocabularySize();
  double averagePageSize();
  
  SuggestInformationLoader getSuggestInformation();
}
