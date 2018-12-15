package com.expleague.sensearch.metrics;

import com.expleague.sensearch.SenSeArch.ResultItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FilterMetric {
    private static String BEGIN_OF_ROOT = "wordstat/query_";

    private ObjectMapper objectMapper = new ObjectMapper();

    public double calc(String queryStr, Set<URI> filterURIs) {
        List<ResultItem> googleResults;
        try {
            googleResults = objectMapper.readValue(
                Files.newInputStream(Paths.get(BEGIN_OF_ROOT + queryStr)),
                new TypeReference<List<ResultItem>>(){}
            );
        } catch (IOException e) {
            System.err.println("No google results for query");
            return -1.;
        }
        Set<URI> googleUris = googleResults
                .stream()
                .map(ResultItem::reference)
                .collect(Collectors.toSet());
        filterURIs.retainAll(googleUris);
        return ((double) filterURIs.size()) / ((double) googleUris.size());
    }
}