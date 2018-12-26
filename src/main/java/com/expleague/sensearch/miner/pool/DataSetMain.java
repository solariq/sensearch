package com.expleague.sensearch.miner.pool;

import com.expleague.commons.random.FastRandom;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.data.tools.Pool;
import com.expleague.ml.meta.DataSetMeta;
import com.expleague.ml.meta.impl.JsonDataSetMeta;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.miner.impl.AccumulatorFeatureSet;
import com.expleague.sensearch.miner.impl.QURLItem;
import com.expleague.sensearch.miner.impl.TargetFeatureSet;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
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
          Stream<Page> sensearchResult = index.fetchDocuments(query);
          sensearchResult.map(page -> new QURLItem(page, query)).forEach(item -> {
            poolBuilder.accept(item);
            poolBuilder.advance();
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
