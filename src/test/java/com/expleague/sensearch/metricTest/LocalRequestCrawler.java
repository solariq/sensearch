package com.expleague.sensearch.metricTest;

import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.SenSeArch.ResultPage;
import com.expleague.sensearch.metrics.WebCrawler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class LocalRequestCrawler implements WebCrawler {

  private final String MAP_FILE = "PAGE.json";
  public String query;
  private Set<String> allTitles;
  private Path pathToMetric;

  @Override
  public List<ResultItem> getGoogleResults(Integer size, String query) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      ResultPage page = objectMapper
          .readValue(pathToMetric.resolve(query).resolve(MAP_FILE).toFile(),
              ResultPage.class);
      return Arrays.asList(page.googleResults());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void setAllTitles(Set<String> allTitles) {
    this.allTitles = allTitles;
  }

  @Override
  public void setPath(Path pathToMetric) {
    this.pathToMetric = pathToMetric;
  }
}
