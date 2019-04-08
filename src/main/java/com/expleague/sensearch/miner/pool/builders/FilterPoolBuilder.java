package com.expleague.sensearch.miner.pool.builders;

import com.expleague.commons.math.Trans;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
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
import com.expleague.sensearch.miner.pool.QueryAndResults.PageAndWight;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    injector.getInstance(FilterPoolBuilder.class).build(Paths.get("./PoolData/filter/"), 1);
  }

  public void build(Path dir, int iteration) {
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

    QueryAndResults[] positiveExamples = positiveData(iteration);
    Map<Query, List<PageAndWight>> data = Arrays.stream(positiveExamples)
        .collect(Collectors.toMap(qNr -> BaseQuery.create(qNr.getQuery(), index),
            qNr -> Arrays.asList(qNr.getAnswers())));
    List<QueryAndResults> newData = new ArrayList<>();

    data.forEach((query, res) -> {
      if (status.get() % 100 == 0) {
        System.err.println(status.get() + " queries completed");
      }
      status.incrementAndGet();
      Map<Page, Features> allDocs =
          index.fetchDocuments(query, FilterMinerPhase.FILTERED_DOC_NUMBER);

      res.forEach(
          pNw -> {
            Page page = index.page(pNw.getUri());
            if (page != PlainPage.EMPTY_PAGE) {
              double target = 0;
              if (pNw.getWight() > 0) {
                target = 1;
              }
              accept(poolBuilder, page, query,
                  ((PlainIndex) index).filterFeatures(query, page.uri()), target);
              allDocs.remove(page);
            }
          });

      List<PageAndWight> newRes = new ArrayList<>(res);
      int tmpAdded = added.get();
      int[] cnt = {0};
      allDocs
          .entrySet()
          .stream()
          .sorted(Comparator.comparingDouble(e -> -model.trans(e.getValue().features()).get(0)))
          .forEach(e -> {
            Page page = e.getKey();
            Features feat = e.getValue();
            if (page == PlainPage.EMPTY_PAGE) {
              return;
            }

            Vec vec = feat.features();
            vec = VecTools.concat(vec, new ArrayVec(0.0));

            if (cnt[0] < FILTER_SIZE) {
              synchronized (poolBuilder) {
                poolBuilder.accept(new QURLItem(page, query),
                    vec, metas);
              }
              if (added.get() - tmpAdded < SAVE_SIZE) {
                added.incrementAndGet();
                newRes.add(new PageAndWight(page.uri().toString(), 0));
              }
              cnt[0]++;
            }
          });
      newData.add(new QueryAndResults(query.text(), newRes));
    });

    saveNewData(newData.toArray(new QueryAndResults[0]), iteration + 1);

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
                } else if (fs instanceof FilterFeatures) {
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
