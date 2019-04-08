package com.expleague.sensearch.miner.pool;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.List;

@JsonPropertyOrder({"query", "answers"})
public class QueryAndResults {

  private String query;
  private PageAndWight[] answers;

  @JsonCreator
  public QueryAndResults(@JsonProperty("query") String query
      , @JsonProperty("answers") List<PageAndWight> answers) {
    this.query = query;
    this.answers = answers.toArray(new PageAndWight[0]);
  }

  @JsonProperty("query")
  public String getQuery() {
    return query;
  }

  @JsonProperty("answers")
  public PageAndWight[] getAnswers() {
    return answers;
  }

  @JsonPropertyOrder({"uri", "weigh"})
  public static class PageAndWight {

    private final URI uri;
    private final double weigh;

    @JsonCreator
    public PageAndWight(@JsonProperty("uri") String uri
        , @JsonProperty("weigh") double weigh) {
      this.uri = URI.create(uri);
      this.weigh = weigh;
    }

    @JsonProperty("uri")
    public URI getUri() {
      return uri;
    }

    @JsonProperty("weigh")
    public double getWight() {
      return weigh;
    }
  }


}
