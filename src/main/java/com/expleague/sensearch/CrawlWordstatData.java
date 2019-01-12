package com.expleague.sensearch;

import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.metrics.RequestCrawler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class CrawlWordstatData {

  private static final Logger LOG = Logger.getLogger(CrawlWordstatData.class.getName());

  public static void main(String[] args) throws IOException {
    Properties logProperties = new Properties();
    logProperties.load(Files.newInputStream(Paths.get("log4j.properties")));
    PropertyConfigurator.configure(logProperties);

    Injector injector = Guice.createInjector(new AppModule());

    RequestCrawler crawler = injector.getInstance(RequestCrawler.class);
    ObjectMapper mapper = new ObjectMapper();
    Random random = new Random(239);


    try (BufferedReader reader = Files.newBufferedReader(Paths.get("wordstat/queries.txt"))) {
      List<String> lines = reader.lines().map(String::trim).collect(Collectors.toList());
      Collections.shuffle(lines);
      lines
          .forEach(
              line -> {
                Path dataPath = Paths.get("wordstat/query_" + line);
                if (Files.exists(dataPath)) {
                  LOG.info("Query " + line + " is already parsed, skipping...");
                  return;
                }
                try {
                  List<ResultItem> googleResults = crawler.getGoogleResults(10, line);
                  mapper.writeValue(
                      Files.newOutputStream(dataPath),
                      googleResults);
                } catch (IOException e) {
                  e.printStackTrace();
                  System.exit(0);
                }

                try {
                  Thread.sleep((int) (Math.abs(random.nextGaussian() + 10) * 1000));
                } catch (InterruptedException e) {
                  e.printStackTrace();
                  System.exit(0);
                }

                LOG.info("Query " + line + " processed");
              });
    }
  }
}
