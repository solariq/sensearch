package com.expleague.sensearch.miner.pool;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.List;

@JsonPropertyOrder({"query", "answers"})
public class QueryAndResults {

  private String query;
  private PageAndRank[] answers;

  @JsonCreator
  public QueryAndResults(@JsonProperty("query") String query
      , @JsonProperty("answers") List<PageAndRank> answers) {
    this.query = query;
    this.answers = answers.toArray(new PageAndRank[0]);
  }

  @JsonProperty("query")
  public String getQuery() {
    return query;
  }

  @JsonProperty("answers")
  public PageAndRank[] getAnswers() {
    return answers;
  }

  @JsonPropertyOrder({"uri", "rank"})
  public static class PageAndRank {

    private final URI uri;
    private final double rank;

    @JsonCreator
    public PageAndRank(@JsonProperty("uri") String uri
        , @JsonProperty("rank") double rank) {
      this.uri = URI.create(uri);
      this.rank = rank;
    }

    @JsonProperty("uri")
    public URI getUri() {
      return uri;
    }

    @JsonProperty("rank")
    public double getRank() {
      return rank;
    }
  }


}
