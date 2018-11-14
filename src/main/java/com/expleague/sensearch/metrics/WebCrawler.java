package com.expleague.sensearch.metrics;

import com.expleague.sensearch.SenSeArch.ResultItem;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public interface WebCrawler {

  public List<ResultItem> getGoogleResults(Integer size, String query);

  public void setAllTitles(Set<String> allTitles);

  public void setPath(Path pathToMetric);
}
