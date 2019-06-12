package com.expleague.sensearch.index;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.web.suggest.SuggestInformationLoader;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public interface Index extends AutoCloseable {
  Map<Page, Features> fetchDocuments(Query query, int cnt);

  Stream<Page> allDocuments();

  @Nullable
  Term term(CharSequence seq);
  Stream<CharSequence> sentences(CharSequence sequence);
  Stream<Term> parse(CharSequence sequence);
  Stream<Term> mostFrequentNeighbours(Term term);
  Vec vecByTerms(List<Term> terms);

  Vec weightedVecByTerms(List<Term> terms);
  Page page(URI reference);

  int size();
  int vocabularySize();
  double averagePageSize();

  double averageSectionTitleSize();

  double averageLinkTargetTitleWordCount();

  Features filterFeatures(Query query, URI pageURI);

  SuggestInformationLoader getSuggestInformation();
}
