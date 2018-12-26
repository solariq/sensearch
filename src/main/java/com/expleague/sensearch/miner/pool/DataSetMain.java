package com.expleague.sensearch.miner.pool;

import com.expleague.commons.random.FastRandom;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.data.tools.Pool;
import com.expleague.ml.meta.DataSetMeta;
import com.expleague.ml.meta.impl.JsonDataSetMeta;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.miner.impl.AccumulatorFeatureSet;
import com.expleague.sensearch.miner.impl.QURLItem;
import com.expleague.sensearch.miner.impl.TargetFeatureSet;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class DataSetMain {

  public static void main(String[] args) {

    try (BufferedReader reader = Files.newBufferedReader(Paths.get("./wordstat/queries.txt"))) {
      Injector injector = Guice.createInjector(new AppModule());
      Index index = injector.getInstance(Index.class);
      FastRandom rand = new FastRandom();
      DataSetMeta meta = new JsonDataSetMeta(
          "Google",
          "sensearch",
          new Date(),
          QURLItem.class,
          rand.nextBase64String(32)
      );
      AccumulatorFeatureSet features = new AccumulatorFeatureSet(index);
      TargetFeatureSet googleTarget = new TargetFeatureSet();

      Pool.Builder<QURLItem> poolBuilder = Pool.builder(meta, features, googleTarget);

      String line;
      while ((line = reader.readLine()) != null) {
        if (Files.exists(Paths.get("./wordstat").resolve("query_" + line))) {
          Query query = BaseQuery.create(line, index);
          Set<String> uniqQURL = new HashSet<>();

          try (BufferedReader queryReader = Files
              .newBufferedReader(Paths.get("./wordstat").resolve("query_" + query.text()))) {
            ObjectMapper objectMapper = new ObjectMapper();
            ResultItem[] res = objectMapper.readValue(queryReader, ResultItemImpl[].class);
            for(ResultItem page : res) {
              uniqQURL.add(page.title().toString());
              poolBuilder.accept(new QURLItem(index.page(page.reference()), query));
              poolBuilder.advance();
            }
          } catch (IOException ignored) {
          }

          Stream<Page> sensearchResult = index.fetchDocuments(query);
          sensearchResult.forEach(page -> {
            if (!uniqQURL.contains(page.title().toString())) {
              uniqQURL.add(page.title().toString());
              poolBuilder.accept(new QURLItem(page, query));
              poolBuilder.advance();
            }
          });
        }
      }
      Pool<QURLItem> pool = poolBuilder.create();
      DataTools.writePoolTo(pool, Files.newBufferedWriter(Paths.get("test.pool")));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
