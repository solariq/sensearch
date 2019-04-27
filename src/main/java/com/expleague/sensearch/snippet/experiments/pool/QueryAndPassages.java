package com.expleague.sensearch.snippet.experiments.pool;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.net.URI;
import java.util.List;

@JsonPropertyOrder({"query", "uri", "answers"})
public class QueryAndPassages {
    private String query;
    private URI uri;
    private PassageAndWeight[] answers;

    @JsonCreator
    public QueryAndPassages(@JsonProperty("query") String query, @JsonProperty("uri") String uri, @JsonProperty("answers") List<PassageAndWeight> answers) {
        this.query = query;
        this.uri = URI.create(uri);
        this.answers = answers.toArray(new PassageAndWeight[0]);
    }

    @JsonProperty("query")
    public String query() {
        return query;
    }

    @JsonProperty("uri")
    public URI uri() {
        return uri;
    }

    @JsonProperty("answers")
    public PassageAndWeight[] answers() {
        return answers;
    }

    @JsonPropertyOrder({"uri", "passage", "weight"})
    public static class PassageAndWeight {

        private final String passage;
        private final double weight;

        @JsonCreator
        public PassageAndWeight(@JsonProperty("passage") String passage, @JsonProperty("weight") double weight) {
            this.passage = passage;
            this.weight = weight;
        }

        @JsonProperty("passage")
        public String passage() {
            return passage;
        }

        @JsonProperty("weight")
        public double weight() {
            return weight;
        }
    }
}
