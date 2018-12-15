package com.expleague.sensearch.metrics;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.core.Annotations;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

public class Metric {

  private final String METRIC_FILE = "METRIC";
  private WebCrawler crawler;
  private Path pathToMetrics;

  @Inject
  public Metric(WebCrawler requestCrawler, @Annotations.MetricPath Path path) {
    crawler = requestCrawler;
    pathToMetrics = path;
    requestCrawler.setPath(path);
  }

  public ResultItem[] calculate(String query, Page[] resultItems) {

    List<String> ourTitles = new ArrayList<>();
    for (Page r : resultItems) {
      ourTitles.add(r.title().toString());
    }
    Path tmpPath = pathToMetrics.resolve(query);
    List<ResultItem> googleResults;

    try {
      Files.createDirectories(tmpPath);
    } catch (IOException e) {
      System.err.println("Can't create directory: " + query);
    }

    try {
      googleResults = crawler.getGoogleResults(ourTitles.size(), query);
    } catch (IOException e) {
      e.printStackTrace();
      return new ResultItem[0];
    }

    double DCG = 0.0;
    int ind = 0;
    for (String title : ourTitles) {
      ResultItem googleResult =
          googleResults
              .stream()
              .filter(item -> item.title().equals(title))
              .findFirst()
              .orElse(null);
      ind++;
      if (googleResult == null) {
        continue;
      }
      double numDouble = googleResults.indexOf(googleResult) + 1;
      numDouble = 1.0 / numDouble;
      DCG += numDouble / (Math.log(1 + ind) / Math.log(2));
    }

    System.err.println("Query: " + query + " DCG: " + DCG);
    try (BufferedWriter DCGWriter =
        new BufferedWriter(
            new OutputStreamWriter(Files.newOutputStream(tmpPath.resolve(METRIC_FILE))))) {
      DCGWriter.write(String.valueOf(DCG));
    } catch (IOException e) {
      e.printStackTrace();
    }

    return googleResults.toArray(new ResultItem[0]);
  }

}
