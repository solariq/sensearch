package com.expleague.sensearch.metrics;

import com.expleague.sensearch.SenSeArch.ResultItem;
import java.nio.file.Path;
import java.util.List;

public interface WebCrawler {

  List<ResultItem> getGoogleResults(Integer size, String query);

  void setPath(Path pathToMetric);
}
