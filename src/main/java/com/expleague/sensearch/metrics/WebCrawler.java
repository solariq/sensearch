package com.expleague.sensearch.metrics;

import com.expleague.sensearch.SenSeArch.ResultItem;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface WebCrawler {

  List<ResultItem> getGoogleResults(Integer size, String query) throws IOException;

  void setPath(Path pathToMetric);
}
