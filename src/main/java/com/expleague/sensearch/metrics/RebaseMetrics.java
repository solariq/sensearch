package com.expleague.sensearch.metrics;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.SenSeArch.ResultPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.io.FileUtils;

public class RebaseMetrics {

  public static void main(String[] args) throws IOException, XMLStreamException {
    Injector injector = Guice.createInjector(new AppModule());
    Config config = injector.getInstance(Config.class);
    SenSeArch searcher = injector.getInstance(SenSeArch.class);

    FileUtils.deleteDirectory(Paths.get("./resources/Metrics").toFile());
    try (BufferedReader reader = Files.newBufferedReader(Paths.get("./resources/Queries.txt"))) {
      String line;
      ObjectMapper objectMapper = new ObjectMapper();
      while ((line = reader.readLine()) != null) {
        ResultPage page = searcher.search(line, 0, false, true);
        if (page.googleResults().length < 10) {
          System.out.println(
              "Too few google results for query "
                  + line
                  + ", found "
                  + page.googleResults().length);
        }
        objectMapper.writeValue(
            config.getPathToMetrics().resolve(line).resolve("PAGE.json").toFile(), page);
      }
    }
  }
}
