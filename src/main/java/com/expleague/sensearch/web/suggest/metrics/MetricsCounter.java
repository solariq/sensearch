package com.expleague.sensearch.web.suggest.metrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import org.apache.log4j.PropertyConfigurator;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.web.suggest.BigramsBasedSuggestor;
import com.expleague.sensearch.web.suggest.OneWordLuceneSuggestor;
import com.expleague.sensearch.web.suggest.RawLuceneSuggestor;
import com.expleague.sensearch.web.suggest.OneWordSuggestor;
import com.expleague.sensearch.web.suggest.Suggestor;
import com.expleague.sensearch.web.suggest.pool.LearnedSuggester;
import com.expleague.sensearch.web.suggest.pool.UnsortedSuggester;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class MetricsCounter {

  private final Suggestor[] suggestors;
  private final int nSugg;

  public MetricsCounter(Suggestor... suggestors) {
    this.suggestors = suggestors;
    nSugg = suggestors.length;
  }

  public void getSuggestsExamples(String... queries) {
    System.out.println("Примеры подсказок:");
    for (String query : queries) {
      System.out.println("|||||||||||||||||||||||||||||| Запрос " + query);
      for (int i = 0; i < nSugg; i++) {
        System.out.println("------------------------ " + suggestors[i].getName());
        List<String> suggests = suggestors[i].getSuggestions(query);
        for (String sugg : suggests) {
          System.out.println(sugg);
        }
      }
    }
    System.out.println("#######################");
  }

  public void evaluate() throws IOException {

    ObjectMapper mapper = new ObjectMapper();
    Map<String, List<String>> map = mapper.readValue(
        Paths.get("sugg_dataset/map").toFile(),
        new TypeReference<Map<String, List<String>>>() {});

    int cnt = 0;
    double[] rrSum = new double[nSugg];
    long[] timeSum = new long[nSugg];
    long[] timeMax = new long[nSugg];
    int[] matched = new int[nSugg];

    for (Entry<String, List<String>> e : map.entrySet()) {

      if (e.getKey().split(" ").length > 1)
        continue;

      for (int i = 0; i < nSugg; i++) {

        long startTime = System.nanoTime();
        List<String> mySugg = suggestors[i].getSuggestions(e.getKey());
        long delta = System.nanoTime() - startTime;
        timeSum[i] += delta;

        if (delta > timeMax[i]) {
          timeMax[i] = delta;
        }

        int pos = 1;
        int maxPos = 0;
        double currRRSum = 0;
        for (String ms : mySugg) {
          for (String os : e.getValue()) {
            //if (ms.equals(os)) {
            if (ms.startsWith(os) || os.startsWith(ms)) {
              matched[i]++;
              currRRSum += 1.0 / pos;
              maxPos = pos;
              System.out.format("Обработано %s / %s, %s, %.4f\n", cnt, map.size(),
                  suggestors[i].getName(), rrSum[i] / (cnt + 1));
              break;
            }
          }
          pos++;
        }
        rrSum[i] += maxPos > 0 ? currRRSum / maxPos : 0;
      }
      cnt++;
    }

    System.out.println("##################");
    for (int i = 0; i < nSugg; i++) {
      System.out.format(
          "Метод %s\n" 
              + "совпадений подсказок %d\n" 
              + "MRR %.3f\n"
              + "Avg. time %.3f\n"
              + "Max time %.3f\n"
              + "--------------------\n",
              suggestors[i].getName(),
              matched[i],
              rrSum[i] / cnt,
              timeSum[i] / cnt / 1e9,
              timeMax[i] / 1e9);
    }

  }

  public static void main(String[] args) throws IOException {

    Properties logProperties = new Properties();
    logProperties.load(Files.newInputStream(Paths.get("log4j.properties")));
    PropertyConfigurator.configure(logProperties);

    Config config =
        new ObjectMapper().readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);
    Injector injector = Guice.createInjector(new AppModule(config));

    Path suggestRoot = config.getIndexRoot().resolve("suggest");
    Index index = injector.getInstance(Index.class);

    MetricsCounter mc = new MetricsCounter(
        //new BigramsBasedSuggestor(index),
        //new OneWordSuggestor(index),
        new RawLuceneSuggestor(suggestRoot),
        new OneWordLuceneSuggestor(index, suggestRoot),
        new LearnedSuggester(index, suggestRoot),
        new DatasetSuggester("map"),
        new DatasetSuggester("map_google"),
        new UnsortedSuggester(index, suggestRoot)
        );

    //mc.getSuggestsExamples("мир");
    mc.getSuggestsExamples("миронов а");
    //mc.evaluate();
  }
}
