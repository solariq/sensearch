package com.expleague.sensearch.web.suggest.pool;

import com.expleague.commons.random.FastRandom;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.data.tools.Pool;
import com.expleague.ml.meta.DataSetMeta;
import com.expleague.ml.meta.impl.JsonDataSetMeta;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.web.suggest.metrics.SuggestsDatasetBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;

public class SuggestRankingPoolBuilder {

  private static final Logger LOG = Logger.getLogger(SuggestRankingPoolBuilder.class.getName());

  private final LearnedSuggester suggestor;

  private final Path suggestIndexRoot;

  public final static Path dataPath = Paths.get("suggest_ranker.pool");

  private final Map<String, List<String>> map;

  private final Random rnd = new Random(15);

  public SuggestRankingPoolBuilder(Index index, Path suggestIndexRoot) throws IOException {
    this.suggestIndexRoot = suggestIndexRoot;
    suggestor = new LearnedSuggester(index, suggestIndexRoot);

    ObjectMapper mapper = new ObjectMapper();

    map = mapper.readValue(SuggestsDatasetBuilder.f, new TypeReference<Map<String, List<String>>>() {});
  }

  public static void main(String[] args) throws IOException {
    Config config =
        new ObjectMapper().readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);
    Injector injector = Guice.createInjector(new AppModule(config));
    Index index = injector.getInstance(Index.class);

    SuggestRankingPoolBuilder pb = new SuggestRankingPoolBuilder(index, config.getIndexRoot().resolve("suggest"));
    pb.build(0);
  }

  public static boolean match(String s1, String s2) {
    return s1.contains(s2) || s2.contains(s1);
  }

  public List<QSUGItem> generateExamples(String partial, List<String> referenceSuggests) {

    List<QSUGItem> res = new ArrayList<>();

    List<QSUGItem> prefixMatched;
    try {
      prefixMatched = suggestor.getUnsortedEndings(partial);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    raw:    for (QSUGItem myMatched : prefixMatched) {

      for (String rs : referenceSuggests) {
        if (match(myMatched.suggestion, rs)) {
          res.add(myMatched.asPositive());
          continue raw;
        }
        if (prefixMatched.size() < 20 || rnd.nextInt(100) < 10) {
          res.add(myMatched);
        }
      }
    }

    return res;
  }

  public void build(int iteration) throws IOException {
    LOG.info("SuggestPool build start");
    long startTime = System.nanoTime();

    FastRandom rand = new FastRandom();
    DataSetMeta meta =
        new JsonDataSetMeta(
            "Yandex", "sensearch", new Date(), QSUGItem.class, rand.nextBase64String(32));
    AccumulatorFeatureSet features = new AccumulatorFeatureSet();
    TargetFeatureSet targetFeatures = new TargetFeatureSet();

    AtomicInteger status = new AtomicInteger();

    Pool.Builder<QSUGItem> poolBuilder = Pool.builder(meta, features, targetFeatures);

    BufferedWriter tabWriter = new BufferedWriter(new PrintWriter(new FileOutputStream(suggestIndexRoot.resolve("ftable.txt").toFile())));
    tabWriter.write("intersection links probCoef cosine phraseLength target\n");

    int exmpNumberSum = 0;
    for(Entry<String, List<String>> qSugList : map.entrySet()) {
      /*
      if (qSugList.getKey().split(" ").length < 2)
        continue;
       */
      if (status.get() % 100 == 0) {
        System.err.println(status.get() + " queries completed");
      }
      status.incrementAndGet();

      //if (status.get() > 4000)
      //  break;
      String query = qSugList.getKey();

      List<QSUGItem> examples = generateExamples(query, qSugList.getValue());
      exmpNumberSum += examples.size();
      for (QSUGItem qsitem : examples) {
        tabWriter.write(
            String.format("%d %d %f %f %f\n",
                qsitem.intersectionLength,
                qsitem.incomingLinksCount,
                qsitem.probabilisticCoeff,
                qsitem.cosine,
                qsitem.vectorSumLength,
                qsitem.isPositive ? 1.0 : 0.0));
        poolBuilder.accept(qsitem);
        poolBuilder.advance();
      }
    }

    System.err.println("Avg. examples number: " + 1.0 * exmpNumberSum / map.size());

    tabWriter.close();
    Pool<QSUGItem> pool = poolBuilder.create();
    DataTools.writePoolTo(pool, Files.newBufferedWriter(suggestIndexRoot.resolve(dataPath)));

    LOG.info(
        String.format(
            "SuggestRankingPool build finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }
}

