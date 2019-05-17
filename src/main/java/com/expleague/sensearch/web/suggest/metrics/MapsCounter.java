package com.expleague.sensearch.web.suggest.metrics;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MapsCounter {
  public static void main(String[] args) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, List<String>> map = mapper.readValue(
        Paths.get("sugg_dataset/map_google").toFile(),
        new TypeReference<Map<String, List<String>>>() {});

    int sumSuggCount = 0;

    for (List<String> vals : map.values()) {
      sumSuggCount += vals.size();
    }

    System.out.format("Test queries: %s, sugg. per query %.2f", map.size(), 1.0 * sumSuggCount / map.size());
  }
}
