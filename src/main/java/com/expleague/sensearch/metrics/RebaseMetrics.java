package com.expleague.sensearch.metrics;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.SenSeArch.ResultPage;
import com.expleague.sensearch.web.Builder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.stream.XMLStreamException;

public class RebaseMetrics {

  public static void main(String[] args) throws IOException, XMLStreamException, URISyntaxException {
    Injector injector = Guice.createInjector(new AppModule());
    Builder builder = injector.getInstance(Builder.class);
    Config config = builder.build();

    try (BufferedReader reader = Files.newBufferedReader(Paths.get("./resources/Queries.txt"))) {
      String line;
      ObjectMapper objectMapper = new ObjectMapper();
      while ((line = reader.readLine()) != null) {
        ResultPage page = builder.getSearcher().search(line, 0);
        objectMapper.writeValue(config.getPathToMetrics().resolve(line).resolve("PAGE.json").toFile(), page);
      }
    }
  }

}
