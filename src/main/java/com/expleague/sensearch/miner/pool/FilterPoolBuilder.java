package com.expleague.sensearch.miner.pool;

import com.expleague.commons.func.Functions;
import com.expleague.commons.random.FastRandom;
import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.data.tools.Pool;
import com.expleague.ml.meta.DataSetMeta;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.impl.JsonDataSetMeta;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.filter.FilterMinerPhase;
import com.expleague.sensearch.filter.features.FilterFeatures;
import com.expleague.sensearch.filter.features.TargetFeatureSet;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.miner.Features;
import com.expleague.sensearch.miner.features.QURLItem;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FilterPoolBuilder {

  private static final int FILTER_SIZE = 100;
  private final Index index;
  private final static FeatureMeta TITLE = FeatureMeta
      .create("dist-title", "cos distance between Query and Title", FeatureMeta.ValueType.VEC);
  private final static FeatureMeta SECTION = FeatureMeta
      .create("dist-section", "cos distance between Query and Nearest Section Body", FeatureMeta.ValueType.VEC);
  private final static FeatureMeta LINK = FeatureMeta
      .create("dist-link", "cos distance between Query and Nearest Incoming Link", FeatureMeta.ValueType.VEC);


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
          ObjectMapper objectMapper = new ObjectMapper();
          Set<CharSeq> validTitles = Arrays.stream(objectMapper.readValue(reader, ResultItemImpl[].class))
              .map(ResultItemImpl::title)
              .map(CharSeq::create)
              .collect(Collectors.toSet());
          Map<Page, Features> allDocs = index.fetchDocuments(query, FilterMinerPhase.FILTERED_DOC_NUMBER);
          allDocs
              .forEach((page, feat) -> {
                if (validTitles.contains(CharSeq.create(page.content(SegmentType.SECTION_TITLE)))) {
                  accept(poolBuilder, page, query, feat);
                }
              });
          final int[] cnt = {0};
          allDocs
              .forEach((page, feat) -> {
                if (cnt[0] == FILTER_SIZE) {
                  return;
                }
                if (!validTitles.contains(CharSeq.create(page.content(SegmentType.SECTION_TITLE)))) {
                  accept(poolBuilder, page, query, feat);
                  cnt[0]++;
                }
              });
        }
      }

      Pool<QURLItem> pool = poolBuilder.create();
      DataTools.writePoolTo(pool, Files.newBufferedWriter(poolPath));
    } catch (IOException ignored) {
    }
  }

  private void accept (Pool.Builder<QURLItem> poolBuilder, Page page, Query query, Features feat) {
    poolBuilder.accept(new QURLItem(page, query));
    poolBuilder.features().map(Functions.cast(FilterFeatures.class))
        .filter(Objects::nonNull)
        .forEach(fs -> {
          fs.withBody(feat.features(SECTION).get(0));
          fs.withLink(feat.features(LINK).get(0));
          fs.withTitle(feat.features(TITLE).get(0));
        });
    poolBuilder.advance();
  }
}
