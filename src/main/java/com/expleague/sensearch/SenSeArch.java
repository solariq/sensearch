package com.expleague.sensearch;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.snippet.Segment;
import java.net.URI;
import java.util.List;

public interface SenSeArch {

  ResultPage search(String query, int pageNo);

  interface ResultPage {

    int number();

    int totalResultsFound();

    ResultItem[] results();

    ResultItem[] googleResults();
  }

  interface ResultItem {

    URI reference();

    CharSequence title();

    List<Pair<CharSequence, List<Segment>>> passages();

    double score();
  }
}
