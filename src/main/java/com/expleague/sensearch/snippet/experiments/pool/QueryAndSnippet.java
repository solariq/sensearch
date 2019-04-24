package com.expleague.sensearch.snippet.experiments.pool;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.net.URI;
import java.util.List;

@JsonPropertyOrder({"query", "answers"})
public class QueryAndSnippet {
    private String query;
    private PassageAndWeight[] answers;

    @JsonCreator
    public QueryAndSnippet(@JsonProperty("query") String query, @JsonProperty("answers") List<PassageAndWeight> answers) {
        this.query = query;
        this.answers = answers.toArray(new PassageAndWeight[0]);
    }

    @JsonProperty("query")
    public String query() {
        return query;
    }

    @JsonProperty
    public PassageAndWeight[] answers() {
        return answers;
    }

    @JsonPropertyOrder({"uri", "passage", "weight"})
    public static class PassageAndWeight {

        private final URI uri;
        private final String passage;
        private final double weight;

        @JsonCreator
        public PassageAndWeight(@JsonProperty("uri") String uri
                , @JsonProperty("passage") String passage, @JsonProperty("weight") double weight) {
            this.uri = URI.create(uri);
            this.passage = passage;
            this.weight = weight;
        }

        @JsonProperty("uri")
        public URI uri() {
            return uri;
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
