package com.expleague.sensearch.miner.pool.builders;

import com.expleague.commons.math.Trans;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.random.FastRandom;
import com.expleague.commons.util.Pair;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.data.tools.Pool;
import com.expleague.ml.data.tools.Pool.Builder;
import com.expleague.ml.meta.DataSetMeta;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.impl.JsonDataSetMeta;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Annotations.RankFilterModel;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.features.sets.TargetFS;
import com.expleague.sensearch.features.sets.TargetSet;
import com.expleague.sensearch.features.sets.filter.FilterFeatures;
import com.expleague.sensearch.filter.FilterMinerPhase;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.index.plain.PlainPage;
import com.expleague.sensearch.miner.pool.QueryAndResults;
import com.expleague.sensearch.miner.pool.QueryAndResults.PageAndRank;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FilterPoolBuilder extends PoolBuilder {

  /* Контракт на данные:
      никакие два запроса не повторяются!
   */

  private Path dir;
  private int SAVE_SIZE = 5;

  private static final int FILTER_SIZE = 10;
  private final Index index;
  private static final FeatureMeta TITLE =
      FeatureMeta.create(
          "dist-title", "cos distance between Query and Title", FeatureMeta.ValueType.VEC);
  private static final FeatureMeta SECTION =
      FeatureMeta.create(
          "dist-section",
          "cos distance between Query and Nearest Section Body",
          FeatureMeta.ValueType.VEC);
  private static final FeatureMeta LINK =
      FeatureMeta.create(
          "dist-link",
          "cos distance between Query and Nearest Incoming Link",
          FeatureMeta.ValueType.VEC);

  private final Trans model;

  @Inject
  public FilterPoolBuilder(Index index, @RankFilterModel Pair<Function, FeatureMeta[]> rankModel) {
    this.index = index;
    this.model = (Trans) rankModel.getFirst();
  }



  public static void main(String[] args) throws IOException {
    Injector injector = Guice.createInjector(new AppModule());
    injector.getInstance(FilterPoolBuilder.class).build(Paths.get("./PoolData/filter/"));
  }

  public void build(Path dir) {
    this.dir = dir;

    FastRandom rand = new FastRandom();
    DataSetMeta meta =
        new JsonDataSetMeta(
            "Google", "sensearch", new Date(), QURLItem.class, rand.nextBase64String(32));
    FilterFeatures features = new FilterFeatures();
    TargetSet targetFeatures = new TargetFS();
    FeatureMeta[] metas = metaData(features, targetFeatures);
    Pool.Builder<QURLItem> poolBuilder = Pool.builder(meta, features, targetFeatures);

    AtomicInteger status = new AtomicInteger(0);
    AtomicInteger added = new AtomicInteger(0);

    QueryAndResults[] positiveExamples = positiveData();
    QueryAndResults[] negativeExmaples = savedData();
    Map<Query, List<PageAndRank>> negativeExmpl = Arrays.stream(negativeExmaples)
        .collect(Collectors.toMap(qNr -> BaseQuery.create(qNr.getQuery(), index)
            , qNr -> Arrays.asList(qNr.getAnswers())));

    Arrays.stream(positiveExamples)
        .parallel()
        .forEach(
            qNr -> {
              if (status.get() % 100 == 0) {
                System.err.println(status.get() + " queries completed");
              }
              status.incrementAndGet();
              Query query = BaseQuery.create(qNr.getQuery(), index);
              Set<URI> used = new HashSet<>();
              Map<Page, Features> allDocs =
                  index.fetchDocuments(query, FilterMinerPhase.FILTERED_DOC_NUMBER);

              Arrays.stream(qNr.getAnswers())
                  .forEach(pNr -> {
                    Page page = index.page(pNr.getUri());
                    if (page != PlainPage.EMPTY_PAGE) {
                      accept(poolBuilder, page, query
                          , ((PlainIndex) index).filterFeatures(query, page.uri())
                          , 1);
                      used.add(page.uri());
                      allDocs.remove(page);
                    }
                  });
              List<PageAndRank> negative = negativeExmpl.get(query);
              negative
                  .forEach(pNr -> {
                    Page page = index.page(pNr.getUri());
                    if (page != PlainPage.EMPTY_PAGE && !used.contains(page.uri())) {
                      accept(poolBuilder, page, query
                          , ((PlainIndex) index).filterFeatures(query, page.uri())
                          , 0);
                      used.add(page.uri());
                      allDocs.remove(page);
                    }
                  });
              int[] cnt = {0};
              allDocs
                  .entrySet()
                  .stream()
                  .sorted(
                      Comparator.comparingDouble(e -> {
                        if (e.getValue().isRequiredInResults()) {
                          return -Double.MAX_VALUE;
                        }
                        return -model.trans(e.getValue().features()).get(0);
                      }))
                  .forEach(
                      (entry) -> {
                        Page page = entry.getKey();
                        Features feat = entry.getValue();
                        if (page == PlainPage.EMPTY_PAGE) {
                          return;
                        }

                        double[] vec = new double[metas.length];
                        for (int f = 0; f < feat.dim(); f++) {
                          vec[f] = feat.features().get(f);
                        }
                        if (cnt[0] < FILTER_SIZE) {
                          vec[feat.dim()] = 0.0;
                          synchronized (poolBuilder) {
                            poolBuilder.accept(new QURLItem(page, query),
                                new ArrayVec(vec), metas);
                          }
                          if (!used.contains(page.uri()) && added.get() < SAVE_SIZE) {
                            added.incrementAndGet();
                            negative.add(new PageAndRank(page.uri().toString(), 0));
                          }
                          cnt[0]++;
                        }
                      });
              negativeExmpl.put(query, negative);
            }
        );

    saveNewData(negativeExmpl.entrySet().stream()
        .map(e -> new QueryAndResults(e.getKey().toString(), e.getValue()))
        .toArray(QueryAndResults[]::new));

    System.out.format("Запомнено новых результатов %d\n", added.get());

    Pool<QURLItem> pool = poolBuilder.create();
    try {
      DataTools.writePoolTo(pool, Files.newBufferedWriter(dir.resolve("filter.pool")));
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }

  private void accept(Builder<QURLItem> poolBuilder, Page page, Query query, Features feat,
      double target) {
    if (page == PlainPage.EMPTY_PAGE) {
      return;
    }
    synchronized (poolBuilder) {
      poolBuilder.accept(new QURLItem(page, query));
      poolBuilder
          .features()
          .forEach(
              fs -> {
                if (fs instanceof TargetFS) {
                  ((TargetFS) fs).acceptTargetValue(target);
                } else if (fs instanceof FilterFeatures){
                  ((FilterFeatures) fs).withBody(feat.features(SECTION).get(0));
                  ((FilterFeatures) fs).withLink(feat.features(LINK).get(0));
                  ((FilterFeatures) fs).withTitle(feat.features(TITLE).get(0));
                }
              }
          );
      poolBuilder.advance();
    }
  }

  @Override
  public Path acceptDir() {
    return dir;
  }
}
