package com.expleague.sensearch.experiments;

import com.expleague.commons.math.Trans;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.util.Pair;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.metrics.Metric;
import com.expleague.sensearch.miner.Features;
import com.expleague.sensearch.miner.features.AccumulatorFeatureSet;
import com.expleague.sensearch.miner.features.QURLItem;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.ranking.RankingPhase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SenSearchDCG {

  private static final Trans model;
  private static final FeatureMeta[] featuresInModel;

  static {
    Pair<Function, FeatureMeta[]> pair =
        DataTools.readModel(
            new InputStreamReader(
                Objects.requireNonNull(
                    RankingPhase.class
                        .getClassLoader()
                        .getResourceAsStream("models/ranking.model")),
                StandardCharsets.UTF_8));
    model = (Trans) pair.getFirst();
    featuresInModel = pair.getSecond();
  }

  public static void main(String[] args) throws IOException {
    try (BufferedReader reader =
        Files.newBufferedReader(
            Paths.get(
                "./src/main/java/com/expleague/sensearch/experiments/queries_for_lucene_experiment"))) {
      Config config =
          new ObjectMapper().readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);
      Injector injector = Guice.createInjector(new AppModule(config));
      Index index = injector.getInstance(Index.class);
      Metric metric =
          new Metric(
              new LocalRequestCrawler(), injector.getInstance(Config.class).getPathToMetrics());
      final AccumulatorFeatureSet features = new AccumulatorFeatureSet(index);

      String line;
      while ((line = reader.readLine()) != null) {
        Query query = BaseQuery.create(line, index);
        final Map<Page, Features> documentsFeatures = new HashMap<>();

        index
            .fetchDocuments(query)
            .forEach(
                page -> {
                  features.accept(new QURLItem(page, query));
                  Vec all = features.advance();
                  documentsFeatures.put(
                      page,
                      new Features() {
                        @Override
                        public Vec features() {
                          return all;
                        }

                        @Override
                        public Vec features(FeatureMeta... metas) {
                          return new ArrayVec(
                              Stream.of(metas)
                                  .mapToInt(features::index)
                                  .mapToDouble(all::get)
                                  .toArray());
                        }

                        @Override
                        public FeatureMeta meta(int index) {
                          return features.meta(index);
                        }

                        @Override
                        public int dim() {
                          return features.dim();
                        }
                      });
                });

        Map<Page, Double> mp =
            documentsFeatures
                .entrySet()
                .stream()
                .collect(
                    Collectors.toMap(
                        Entry::getKey, p -> rank(p.getValue().features(featuresInModel))));

        Page[] pages =
            mp.entrySet()
                .stream()
                .map(p -> Pair.create(p.getKey(), p.getValue()))
                .sorted(Comparator.<Pair<Page, Double>>comparingDouble(Pair::getSecond).reversed())
                .map(Pair::getFirst)
                .limit(10)
                .toArray(Page[]::new);
        metric.calculate(line, pages);
      }
    }
  }

  private static double rank(Vec features) {
    /*double vec = 0;
    for (int ind = 0;  ind < features.dim(); ind++) {
      double normalize = 1.0 / (ind + 1);
      double f =features.get(ind);
      vec += (f * normalize);
    }
    return vec;*/
    return model.trans(features).get(0);
  }
}
