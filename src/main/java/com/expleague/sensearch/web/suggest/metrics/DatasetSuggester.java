package com.expleague.sensearch.web.suggest.metrics;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.expleague.sensearch.web.suggest.Suggestor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DatasetSuggester implements Suggestor {

  private final int ret_limit = 10;

  private final String name;
  private final Map<String, List<String>> map;

  public DatasetSuggester(String datasetFileName) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    map = mapper.readValue(
        Paths.get("sugg_dataset/" + datasetFileName).toFile(),
        new TypeReference<Map<String, List<String>>>() {});
    
    name = "Dataset " + datasetFileName;
  }

  @Override
  public String getName() {
    return name;
  }
  
  @Override
  public List<String> getSuggestions(String searchString) {
    List<String> sugg = map.get(searchString);
    if (sugg == null) {
      return Collections.emptyList();
    }
    if (sugg.size() <= ret_limit) {
      return sugg;
    } else {
      return sugg;//.subList(0, ret_limit);
    }
  }

}
