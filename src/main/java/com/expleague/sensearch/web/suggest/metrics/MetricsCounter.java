package com.expleague.sensearch.web.suggest.metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import org.apache.log4j.PropertyConfigurator;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.web.suggest.Suggestor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class MetricsCounter {
  public static void main(String[] args) throws IOException {

    Properties logProperties = new Properties();
    logProperties.load(Files.newInputStream(Paths.get("log4j.properties")));
    PropertyConfigurator.configure(logProperties);

    Config config =
        new ObjectMapper().readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);
    Injector injector = Guice.createInjector(new AppModule(config));

    Suggestor suggestor = injector.getInstance(Suggestor.class);

    ObjectMapper mapper = new ObjectMapper();
    Map<String, List<String>> map = mapper.readValue(
        Paths.get("sugg_dataset/map").toFile(),
        new TypeReference<Map<String, List<String>>>() {});

    double rrSum = 0;
    int matched = 0, cnt = 0;
    for (Entry<String, List<String>> e : map.entrySet()) {
      List<String> mySugg = suggestor.getSuggestions(e.getKey());
      int pos = 1;
      for (String ms : mySugg) {
        for (String os : e.getValue()) {
          if (ms.startsWith(os)) {
            matched++;
            rrSum += 1.0 / pos;
            System.out.println("####\n" + e.getKey() + "\n"
                + ms + "\n"
                    + os);
            System.out.format(
                "Всего запросов %d\n" + "Обработано %d\n" + "совпадений подсказок %d\n" + "MRR %.3f\n",
                map.size(), cnt, matched, rrSum / cnt);
          }
        }
        pos++;
      }
      cnt++;
      /*System.out.println(e.getKey() + " processed");
      System.out.format(
          "Всего запросов %d\n" + "Обработано %d\n" + "совпадений подсказок %d\n" + "MRR %.3f\n",
          map.size(), cnt, matched, rrSum / cnt);*/
    }

  }
}
