package com.expleague.sensearch.metricTest;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.SenSeArch;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class MetricTest {

  private SenSeArch searcher;
  private final LocalRequestCrawler webCrawler = new LocalRequestCrawler();

  @Before
  public void initSearch() {
    Injector injector = Guice.createInjector(new AppModule());
    searcher = injector.getInstance(SenSeArch.class);
  }

  @Test
  public void metricTest() {
    Path pathToMetric = Paths.get("./resources/Metrics");
    try (BufferedReader reader = Files.newBufferedReader(Paths.get("./resources/Queries.txt"))) {
      String line;
      while ((line = reader.readLine()) != null) {
        webCrawler.query = line;
        BufferedReader readOld =
            Files.newBufferedReader(pathToMetric.resolve(line).resolve("METRIC"));
        System.err.println(readOld.readLine());
        searcher.search(line, 0, false, true);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
