package com.expleague.sensearch.metricTest;

import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.metrics.WebCrawler;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class LocalRequestCrawler implements WebCrawler {
  private final String MAP_FILE = "MAP";
  private Set<String> allTitles;
  private Path pathToMetric;

  @Override
  public List<ResultItem> getGoogleResults(Integer size, String query) {

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
