package com.expleague.sensearch.miner.pool;

import com.expleague.commons.func.Functions;
import com.expleague.commons.random.FastRandom;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.data.tools.Pool;
import com.expleague.ml.meta.DataSetMeta;
import com.expleague.ml.meta.impl.JsonDataSetMeta;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.index.plain.features.FilterFeatures;
import com.expleague.sensearch.index.plain.features.TargetFeatureSet;
import com.expleague.sensearch.miner.features.QURLItem;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class FilterPoolBuilder {

  private static final int FILTER_SIZE = 2000;
  private final Index index;

  @Inject
  public FilterPoolBuilder(Index index) {
    this.index = index;
  }

  public static void main(String[] args) throws IOException {
    Injector injector = Guice.createInjector(new AppModule());
    injector.getInstance(FilterPoolBuilder.class).build(Paths.get("filter.pool"));
  }

  public void build(Path poolPath) {
    try (BufferedReader reader = Files.newBufferedReader(Paths.get("./wordstat/queries.txt"))) {
      FastRandom rand = new FastRandom();
      DataSetMeta meta =
          new JsonDataSetMeta(
              "Google", "sensearch", new Date(), QURLItem.class, rand.nextBase64String(32));
      List<String> queries = new ArrayList<>();
      String line;
      while ((line = reader.readLine()) != null) {
        queries.add(line);
      }
      FilterFeatures features = new FilterFeatures();
      TargetFeatureSet targetFeatureSet = new TargetFeatureSet();

      Pool.Builder<QURLItem> poolBuilder = Pool.builder(meta, features, targetFeatureSet);

      for (int q = 0; q < queries.size(); q++) {
        String queryString = queries.get(q);
        if (Files.exists(Paths.get("./wordstat").resolve("query_" + queryString))) {
          Query query = BaseQuery.create(queryString, index);
          ((PlainIndex)index).fetchDocuments(query, FILTER_SIZE).map(p -> (IndexedPage) p)
              .forEach(page -> {
                poolBuilder.accept(new QURLItem(page, query));
                poolBuilder.features().map(Functions.cast(FilterFeatures.class))
                    .filter(Objects::nonNull)
                    .forEach(fs -> {
                      fs.withBody(page.getBodyDist());
                      fs.withLink(page.getLinkDist());
                      fs.withTitle(page.getTitleDist());
                    });
                poolBuilder.advance();
              }
          );
        }
      }
      Pool<QURLItem> pool = poolBuilder.create();
      DataTools.writePoolTo(pool, Files.newBufferedWriter(poolPath));
    } catch (IOException ignored) {
    }
  }


}
