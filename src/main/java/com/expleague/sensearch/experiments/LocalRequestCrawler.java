package com.expleague.sensearch.experiments;

import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.SenSeArch.ResultPage;
import com.expleague.sensearch.metrics.WebCrawler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocalRequestCrawler implements WebCrawler {

  private final String MAP_FILE = "PAGE.json";
  String query;
  private Path pathToMetric;

  @Override
  public List<ResultItem> getGoogleResults(Integer size, String query) {
    Path pathToWordstat = Paths.get("./wordstat");
    pathToWordstat = pathToWordstat.resolve("query_" + query);


    ObjectMapper objectMapper = new ObjectMapper();
    try {
      System.err.println();
          ResultItem[] page =
          objectMapper.readValue(
              pathToWordstat.toFile(), ResultItem[].class);
//              pathToMetric.resolve(query).resolve(MAP_FILE).toFile(), ResultPage.class);
      return Arrays.asList(page);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void setPath(Path pathToMetric) {
    this.pathToMetric = pathToMetric;
  }
}
