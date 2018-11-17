package com.expleague.sensearch.metricTest;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.web.Builder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.stream.XMLStreamException;
import org.junit.Before;
import org.junit.Test;

public class MetricTest {

  private Builder builder;
  private Config config;
  private SenSeArch searcher;
  private LocalRequestCrawler webCrawler = new LocalRequestCrawler();

  @Before
  public void initSearch() throws IOException, XMLStreamException {
    Injector injector = Guice.createInjector(new AppModule());
    builder = injector.getInstance(Builder.class);
    config = builder.build(webCrawler);
    searcher = builder.getSearcher();
  }

  @Test
  public void metricTest() {
    try (BufferedReader reader = Files.newBufferedReader(Paths.get("./resources/Queries.txt"))) {
      String line;
      while ((line = reader.readLine()) != null) {
        webCrawler.query = line;
        builder.getSearcher().search(line, 0);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
