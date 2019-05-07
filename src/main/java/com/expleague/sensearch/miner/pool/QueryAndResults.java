package com.expleague.sensearch.miner.pool;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.List;

@JsonPropertyOrder({"query", "answers"})
public class QueryAndResults {

  private String query;
  private PageAndWeight[] answers;

  @JsonCreator
  public QueryAndResults(@JsonProperty("query") String query
      , @JsonProperty("answers") List<PageAndWeight> answers) {
    this.query = query;
    this.answers = answers.toArray(new PageAndWeight[0]);
  }

  @JsonProperty("query")
  public String getQuery() {
    return query;
  }

  @JsonProperty("answers")
  public PageAndWeight[] getAnswers() {
    return answers;
  }

  @JsonPropertyOrder({"uri", "weight"})
  public static class PageAndWeight {

    private final URI uri;
    private final double weight;

    @JsonCreator
    public PageAndWeight(@JsonProperty("uri") String uri
            , @JsonProperty("weight") double weight) {
      this.uri = URI.create(uri);
      this.weight = weight;
    }

    @JsonProperty("uri")
    public URI getUri() {
      return uri;
    }

    @JsonProperty("weight")
    public double getWeight() {
      return weight;
    }
  }


}
