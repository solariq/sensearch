package com.expleague.sensearch;

import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.metrics.RequestCrawler;
import com.expleague.sensearch.web.Builder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import javax.xml.stream.XMLStreamException;
import org.apache.log4j.PropertyConfigurator;

public class CrawlWordstatData {

  public static void main(String[] args) throws IOException, XMLStreamException {
    Properties logProperties = new Properties();
    logProperties.load(Files.newInputStream(Paths.get("log4j.properties")));
    PropertyConfigurator.configure(logProperties);

    Injector injector = Guice.createInjector(new AppModule());
    Builder builder = injector.getInstance(Builder.class);
    Config config = builder.build();

    RequestCrawler crawler = new RequestCrawler(builder.getIndex());
    ObjectMapper mapper = new ObjectMapper();
    Random random = new Random(239);

    // If Google bans us, then update this number and restart
    int[] queryNum = new int[]{93};

    try (BufferedReader reader = Files.newBufferedReader(Paths.get("wordstat/queries.txt"))) {
      reader
          .lines()
          .skip(queryNum[0])
          .forEach(
              line -> {
                try {
                  List<ResultItem> googleResults = crawler.getGoogleResults(10, line.trim());
                  mapper.writeValue(
                      Files.newOutputStream(Paths.get("wordstat/query_" + line.trim())),
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

                queryNum[0]++;
                System.out.println(
                    String.format("Query %s (%d / 2000) processed", line, queryNum[0]));
              });
    }
  }
}
