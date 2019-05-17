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
import com.expleague.sensearch.web.suggest.OneWordLuceneLinks;
import com.expleague.sensearch.web.suggest.OneWordLuceneSuggestor;
import com.expleague.sensearch.web.suggest.OneWordLuceneTFIDF;
import com.expleague.sensearch.web.suggest.RawLuceneSuggestor;
import com.expleague.sensearch.web.suggest.OneWordSuggestor;
import com.expleague.sensearch.web.suggest.Suggestor;
import com.expleague.sensearch.web.suggest.pool.LearnedSuggester;
import com.expleague.sensearch.web.suggest.pool.SuggestRankingPoolBuilder;
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
          System.out.println("|" + sugg + "|");
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
    double[] mapSum = new double[nSugg];
    long[] timeSum = new long[nSugg];
    long[] timeMax = new long[nSugg];
    int[] matched = new int[nSugg];

    for (Entry<String, List<String>> e : map.entrySet()) {

      String[] words = e.getKey().split(" ");
      if (words.length < 2)
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
        boolean firstMatched = false;
        int current_matched = 0;
        for (String ms : mySugg) {
          for (String os : e.getValue()) {
            //if (ms.equals(os)) {
            //if (ms.startsWith(os) || os.startsWith(ms)) {
            if (SuggestRankingPoolBuilder.match(ms, os)) {
              matched[i]++;
              current_matched++;
              if (!firstMatched) {
                rrSum[i] += 1.0 / pos;
              }
              firstMatched = true;

              break;
            }
          }
          pos++;
        }
        mapSum[i] += mySugg.size() > 0 ? (1.0 * current_matched / mySugg.size()) : 0;
        System.out.format("Обработано %s / %s, %s, MRR: %.4f MAP %.4f\n", cnt, map.size(),
            suggestors[i].getName(),
            rrSum[i] / (cnt + 1),
            mapSum[i] / (cnt + 1));
      }
      cnt++;
    }

    System.out.println("##################");
    for (int i = 0; i < nSugg; i++) {
      System.out.format(
          "Метод %s\n" 
              + "совпадений подсказок %d\n" 
              + "MRR %.3f\n"
              + "MAP %.3f\n"
              + "Avg. time %.3f\n"
              + "Max time %.3f\n"
              + "--------------------\n",
              suggestors[i].getName(),
              matched[i],
              rrSum[i] / cnt,
              mapSum[i] / cnt,
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
        //new RawLuceneSuggestor(suggestRoot),
        new OneWordLuceneSuggestor(index, suggestRoot),
        //new OneWordLuceneTFIDF(index, suggestRoot),
        //new OneWordLuceneLinks(index, suggestRoot),
        new LearnedSuggester(index, suggestRoot)
        //new DatasetSuggester("map"),
        //new DatasetSuggester("map_google")
        //new UnsortedSuggester(index, suggestRoot)
        );

    mc.getSuggestsExamples("мир");
    mc.getSuggestsExamples("миронов а");
    mc.evaluate();
  }
}
