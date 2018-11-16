package com.expleague.sensearch;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.core.deserializers.ResultItemDeserializer;
import com.expleague.sensearch.core.deserializers.ResultPageDeserializer;
import com.expleague.sensearch.snippet.Segment;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.net.URI;
import java.util.List;

public interface SenSeArch {

  ResultPage search(String query, int pageNo);

  @JsonDeserialize(using = ResultPageDeserializer.class)
  interface ResultPage {

    int number();

    int totalResultsFound();

    ResultItem[] results();

    ResultItem[] googleResults();
  }

  @JsonDeserialize(using = ResultItemDeserializer.class)
  interface ResultItem {

    URI reference();

    CharSequence title();

    List<Pair<CharSequence, List<Segment>>> passages();

    double score();
  }
}
