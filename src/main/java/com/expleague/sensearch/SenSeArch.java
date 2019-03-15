package com.expleague.sensearch;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.core.impl.json.ResultItemDebugInfoSerializer;
import com.expleague.sensearch.core.impl.json.ResultItemDeserializer;
import com.expleague.sensearch.core.impl.json.ResultItemSerializer;
import com.expleague.sensearch.core.impl.json.ResultPageDeserializer;
import com.expleague.sensearch.core.impl.json.ResultPageSerializer;
import com.expleague.sensearch.core.impl.json.SynonymInfoSerializer;
import com.expleague.sensearch.snippet.Segment;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.net.URI;
import java.util.List;

public interface SenSeArch {

  ResultPage search(String query, int pageNo, boolean debug, boolean metric);

  List<SynonymInfo> synonyms(String uri, String query);

  @JsonSerialize(using = ResultPageSerializer.class)
  @JsonDeserialize(using = ResultPageDeserializer.class)
  interface ResultPage {

    int number();

    int totalResultsFound();

    String query();

    ResultItem[] results();

    ResultItem[] googleResults();
  }

  @JsonSerialize(using = ResultItemSerializer.class)
  @JsonDeserialize(using = ResultItemDeserializer.class)
  interface ResultItem {

    URI reference();

    CharSequence title();

    List<Pair<CharSequence, List<Segment>>> passages();

    double score();

    ResultItemDebugInfo debugInfo();
  }

  @JsonSerialize(using = ResultItemDebugInfoSerializer.class)
  interface ResultItemDebugInfo {

    String uri();

    /**
     * @return rank of this result or -1 if it's filtered out
     */

    int rank();

    double[] features();

    String[] featureIds();
  }

  @JsonSerialize(using = SynonymInfoSerializer.class)
  interface SynonymInfo {

    String queryWord();

    SynonymAndScore[] queryWordSynonyms();
  }

  interface SynonymAndScore {

    double score();

    String synonym();
  }


}
