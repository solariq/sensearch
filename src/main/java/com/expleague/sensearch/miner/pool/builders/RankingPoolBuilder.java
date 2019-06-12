package com.expleague.sensearch.miner.pool.builders;

import com.expleague.commons.func.Functions;
import com.expleague.commons.math.Trans;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.commons.random.FastRandom;
import com.expleague.commons.util.Pair;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.data.tools.Pool;
import com.expleague.ml.meta.DataSetMeta;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.impl.JsonDataSetMeta;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Annotations.FilterMinerDocNum;
import com.expleague.sensearch.core.Annotations.RankModel;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.features.sets.TargetFS;
import com.expleague.sensearch.features.sets.TargetSet;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainPage;
import com.expleague.sensearch.miner.AccumulatorMinerFeatureSet;
import com.expleague.sensearch.miner.pool.QueryAndResults;
import com.expleague.sensearch.miner.pool.QueryAndResults.PageAndWeight;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import org.apache.log4j.Logger;

public class RankingPoolBuilder extends PoolBuilder<QueryAndResults> {

  private static final Logger LOG = Logger.getLogger(FilterPoolBuilder.class.getName());
  private int SAVE_SIZE = 5;

  private static final int RANK_DOCUMENTS = 10;
  private final Index index;
  private final Trans model;
  private final int filterDocNum;

  @Inject
  public RankingPoolBuilder(Index index, @RankModel Pair<Function, FeatureMeta[]> rankModel,
      @FilterMinerDocNum int filterDocNum) {
    this.index = index;
    this.model = (Trans) rankModel.getFirst();
    this.filterDocNum = filterDocNum;
  }

  public static void main(String[] args) throws IOException {
    Injector injector = Guice.createInjector(new AppModule());
    injector.getInstance(RankingPoolBuilder.class).build(Paths.get("./PoolData/ranker/"), 0);
  }

  public void build(Path dataPath, int iteration) {
    LOG.info("RankingPool build start");
    long startTime = System.nanoTime();

    FastRandom rand = new FastRandom();
    DataSetMeta meta =
        new JsonDataSetMeta(
            "Google", "sensearch", new Date(), QURLItem.class, rand.nextBase64String(32));
    AccumulatorMinerFeatureSet features = new AccumulatorMinerFeatureSet(index);
    TargetSet targetFeatures = new TargetFS();
    FeatureMeta[] metas = metaData(features, targetFeatures);
    Pool.Builder<QURLItem> poolBuilder = Pool.builder(meta, features, targetFeatures);

    AtomicInteger status = new AtomicInteger(0);
    AtomicInteger added = new AtomicInteger(0);

    QueryAndResults[] positiveExamples = readData(QueryAndResults.class, iteration, dataPath);
    List<QueryAndResults> newData = Collections.synchronizedList(new ArrayList<>());
    Arrays.stream(positiveExamples)
        .parallel()
        .forEach(
            qNr -> {
              final int[] tmpAdded = {0};
              if (status.get() % 100 == 0) {
                System.err.println(status.get() + " queries completed");
              }
              status.incrementAndGet();

              Query query = BaseQuery.create(qNr.getQuery(), index);
              List<PageAndWeight> res = Arrays.asList(qNr.getAnswers());

              Map<Page, Features> allDocs =
                  index.fetchDocuments(query, filterDocNum);

              res.forEach(
                  pNw -> {
                    Page page = index.page(pNw.getUri());
                    if (page != PlainPage.EMPTY_PAGE) {
                      double target = pNw.getWeight();
                      synchronized (poolBuilder) {
                        poolBuilder.accept(new QURLItem(page, query));
                        poolBuilder
                            .features()
                            .map(Functions.cast(TargetFS.class))
                            .filter(Objects::nonNull)
                            .forEach(fs -> fs.acceptTargetValue(target));
                        poolBuilder.advance();
                      }
                      allDocs.remove(page);
                    }
                  });

              List<PageAndWeight> newRes = new ArrayList<>(res);

              Map<Page, Vec> feat = new HashMap<>();
              AccumulatorMinerFeatureSet AFS = new AccumulatorMinerFeatureSet(index);

              int[] cnt = {0};
              allDocs
                  .keySet()
                  .stream()
                  .sorted(
                      Comparator.comparingDouble(
                          new ToDoubleFunction<Page>() {
                            private Map<Long, Double> computedValues = new HashMap<>();

                            @Override
                            public double applyAsDouble(Page page) {
                              Double res = computedValues.get(((PlainPage) page).id());
                              if (res != null) {
                                return res;
                              }
//                              LOG.info("RankingPoolBuilder mining features");
//                              long time = System.nanoTime();
                              AFS.accept(new QURLItem(page, query));
                              Vec all = AFS.advance();
//                              LOG.info(String.format("RankingPoolBuilder finished mining in %.3f seconds", (System.nanoTime() - time) / 1e9));
                              feat.put(page, all);
                              res = -model.trans(all).get(0);
                              computedValues.put(((PlainPage) page).id(), res);
                              return res;
                            }
                          }))
                  .forEach(
                      page -> {
                        if (page == PlainPage.EMPTY_PAGE) {
                          return;
                        }

                        Vec vec = feat.get(page);
                        vec = VecTools.concat(vec, new ArrayVec(0.0));

                        if (cnt[0] < RANK_DOCUMENTS) {
                          synchronized (poolBuilder) {
                            poolBuilder.accept(new QURLItem(page, query), vec, metas);
                          }
                          if (tmpAdded[0] < SAVE_SIZE) {
                            tmpAdded[0]++;
                            newRes.add(new PageAndWeight(page.uri().toString(), 0));
                          }
                          cnt[0]++;
                        }
                      });
              added.addAndGet(tmpAdded[0]);
              newData.add(new QueryAndResults(query.text(), newRes));
            });

    saveNewIterationData(dataPath, newData.toArray(new QueryAndResults[0]), iteration + 1);

    LOG.info(String.format("Memorized %d new results\n", added.get()));

    Pool<QURLItem> pool = poolBuilder.create();
    try {
      DataTools.writePoolTo(pool, Files.newBufferedWriter(dataPath.resolve("ranker.pool")));
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
    LOG.info(
        String.format(
            "RankingPool build finished in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
  }
}
