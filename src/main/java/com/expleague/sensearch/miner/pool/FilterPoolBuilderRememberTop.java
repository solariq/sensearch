package com.expleague.sensearch.miner.pool;

import com.expleague.commons.func.Functions;
import com.expleague.commons.math.Trans;
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
import com.expleague.sensearch.core.Annotations.RankFilterModel;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.features.sets.filter.FilterFeatures;
import com.expleague.sensearch.features.sets.filter.TargetFeatureSet;
import com.expleague.sensearch.filter.FilterMinerPhase;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.index.plain.PlainPage;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.Function;

public class FilterPoolBuilderRememberTop extends RememberTopPoolBuilder {

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
  public FilterPoolBuilderRememberTop(
      Index index, @RankFilterModel Pair<Function, FeatureMeta[]> rankModel) {
    this.index = index;
    this.model = (Trans) rankModel.getFirst();
  }

  public static void main(String[] args) throws IOException {
    Injector injector = Guice.createInjector(new AppModule());
    injector.getInstance(FilterPoolBuilderRememberTop.class).build(Paths.get("filter.pool"));
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
      FeatureMeta[] metas = new FeatureMeta[features.dim() + targetFeatureSet.dim()];
      for (int f = 0; f < features.dim(); f++) {
        metas[f] = features.meta(f);
      }
      for (int f = 0; f < targetFeatureSet.dim(); f++) {
        metas[f + features.dim()] = targetFeatureSet.meta(f);
      }

      Pool.Builder<QURLItem> poolBuilder = Pool.builder(meta, features, targetFeatureSet);

      AtomicInteger status = new AtomicInteger(0);

      AtomicIntegerArray cntAddedSaved = new AtomicIntegerArray(2);
      queries
          .parallelStream()
          .forEach(
              queryString -> {
                try {
                  if (status.get() % 100 == 0) {
                    System.err.println(status.get() + " queries completed");
                  }
                  if (Files.exists(Paths.get("./wordstat").resolve("query_" + queryString))) {
                    status.incrementAndGet();
                    BufferedReader readerFile =
                        Files.newBufferedReader(
                            Paths.get("./wordstat").resolve("query_" + queryString));

                    Query query = BaseQuery.create(queryString, index);
                    ObjectMapper objectMapper = new ObjectMapper();

                    Map<Page, Features> allDocs =
                        index.fetchDocuments(query, FilterMinerPhase.FILTERED_DOC_NUMBER);

                    List<URI> rememberedURIs = getSavedQueryTop(queryString);

                    rememberedURIs.forEach(
                        uri -> {
                          Page page = index.page(uri);
                          if (page != PlainPage.EMPTY_PAGE) {
                            accept(
                                poolBuilder,
                                page,
                                query,
                                ((PlainIndex) index).filterFeatures(query, uri));
                            allDocs.remove(page);
                          }
                        });

                    Arrays.stream(objectMapper.readValue(readerFile, ResultItemImpl[].class))
                        .map(ResultItemImpl::reference)
                        .forEach(
                            uri -> {
                              Page page = index.page(uri);
                              if (page == PlainPage.EMPTY_PAGE
                                  || rememberedURIs.contains(uri)) {
                                return;
                              }
                              accept(
                                  poolBuilder,
                                  page,
                                  query,
                                  ((PlainIndex) index).filterFeatures(query, uri));
                              allDocs.remove(page);
                            });

                    final int[] cnt = {0};
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

                              if (page == PlainPage.EMPTY_PAGE || rememberedURIs.contains(page.uri())) {
                                return;
                              }

                              double[] vec = new double[metas.length];
                              for (int f = 0; f < feat.dim(); f++) {
                                vec[f] = feat.features().get(f);
                              }
                              if (cnt[0] < FILTER_SIZE) {
                                cntAddedSaved.incrementAndGet(0);
                                rememberedURIs.add(page.uri());
                                vec[feat.dim()] = 0.0;
                                synchronized (poolBuilder) {
                                  poolBuilder.accept(new QURLItem(page, query),
                                      new ArrayVec(vec),
                                      metas);
                                }
//                                accept(poolBuilder, page, query, feat);
                                cnt[0]++;
                              }
                            });

                    cntAddedSaved.addAndGet(1, rememberedURIs.size());
                    saveQueryTop(queryString, rememberedURIs);
                  }
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });

      System.out.format(
          "Запомнено новых результатов %d\n" + "Всего запомнено %d\n",
          cntAddedSaved.get(0), cntAddedSaved.get(1));

      Pool<QURLItem> pool = poolBuilder.create();
      DataTools.writePoolTo(pool, Files.newBufferedWriter(poolPath));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void accept(Pool.Builder<QURLItem> poolBuilder, Page page, Query query, Features feat) {
    if (page == PlainPage.EMPTY_PAGE) {
      return;
    }
    synchronized (poolBuilder) {
      poolBuilder.accept(new QURLItem(page, query));
      poolBuilder
          .features()
          .map(Functions.cast(FilterFeatures.class))
          .filter(Objects::nonNull)
          .forEach(
              fs -> {
                fs.withBody(feat.features(SECTION).get(0));
                fs.withLink(feat.features(LINK).get(0));
                fs.withTitle(feat.features(TITLE).get(0));
              });
      poolBuilder.advance();
    }
  }

  @Override
  public Path getRememberDir() {
    return Paths.get("pbdata/filterPhaseTop/");
  }
}
