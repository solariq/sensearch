package com.expleague.sensearch.metrics;

import com.expleague.sensearch.SenSeArch.ResultItem;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public interface WebCrawler {

  public List<ResultItem> getGoogleResults(Integer size, String query);

  public void setAllTitles(Stream<CharSequence> allTitles);

  public void setPath(Path pathToMetric);
}
