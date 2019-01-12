package com.expleague.sensearch.index;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.web.suggest.SuggestInformationLoader;
import java.net.URI;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public interface Index {
  Stream<Page> fetchDocuments(Query query);

  @Nullable
  Term term(CharSequence seq);
  Stream<CharSequence> sentences(CharSequence sequence);
  Stream<Term> parse(CharSequence sequence);
  Stream<Term> mostFrequentNeighbours(Term term);

  Page page(URI reference);

  int size();
  int vocabularySize();
  double averagePageSize();
  double averageTitleSize();
  double averageLinkSize();
  
  SuggestInformationLoader getSuggestInformation();
}
