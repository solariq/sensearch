package com.expleague.sensearch.miner.pool;

import com.expleague.commons.random.FastRandom;
import com.expleague.commons.seq.CharSeq;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.data.tools.Pool;
import com.expleague.ml.data.tools.Pool.Builder;
import com.expleague.ml.meta.DataSetMeta;
import com.expleague.ml.meta.impl.JsonDataSetMeta;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.core.impl.ResultItemImpl;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.features.sets.ranker.TargetFeatureSet;
import com.expleague.sensearch.filter.FilterMinerPhase;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.miner.AccumulatorFeatureSet;
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
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class RankingPoolBuilder {

  private static final int RANK_DOCUMENTS = 100;
  private final Index index;

  @Inject
  public RankingPoolBuilder(Index index) {
    this.index = index;
  }

  public static void main(String[] args) throws IOException {
    Injector injector = Guice.createInjector(new AppModule());
    injector.getInstance(RankingPoolBuilder.class).build(Paths.get("ranking.pool"));
  }

  public void build(Path poolPath) {
    FastRandom rand = new FastRandom();
    DataSetMeta meta =
        new JsonDataSetMeta(
            "Google", "sensearch", new Date(), QURLItem.class, rand.nextBase64String(32));
    AccumulatorFeatureSet features = new AccumulatorFeatureSet(index);
    TargetFeatureSet googleTarget = new TargetFeatureSet();

    Builder<QURLItem> poolBuilder = Pool.builder(meta, features, googleTarget);

    AtomicInteger status = new AtomicInteger();
    try {
      Files.readAllLines(Paths.get("./wordstat/queries.txt"))
          .stream()
          .parallel()
          .forEach(
              line -> {
                if (status.get() % 100 == 0) {
                  System.err.println(status + " queries completed");
                }
                if (Files.exists(Paths.get("./wordstat").resolve("query_" + line))) {
                  status.incrementAndGet();
                  Query query = BaseQuery.create(line, index);
                  Set<CharSeq> uniqQURL = new HashSet<>();

                  try (BufferedReader queryReader =
                      Files.newBufferedReader(
                          Paths.get("./wordstat").resolve("query_" + query.text()))) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    ResultItem[] res = objectMapper.readValue(queryReader, ResultItemImpl[].class);
                    synchronized (poolBuilder) {
                      for (ResultItem page : res) {
                        uniqQURL.add(CharSeq.create(page.title()));
                        poolBuilder.accept(new QURLItem(index.page(page.reference()), query));
                        poolBuilder.advance();
                      }
                    }
                  } catch (IOException e) {
                    e.printStackTrace();
                  }

                  Stream<Page> sensearchResult =
                      index
                          .fetchDocuments(query, FilterMinerPhase.FILTERED_DOC_NUMBER)
                          .keySet()
                          .stream()
                          .filter(
                              page ->
                                  !uniqQURL.contains(
                                      CharSeq.create(page.content(SegmentType.SECTION_TITLE))))
                          .limit(RANK_DOCUMENTS);
                  synchronized (poolBuilder) {
                    sensearchResult.forEach(
                        page -> {
                          if (!uniqQURL.contains(
                              CharSeq.create(page.content(SegmentType.SECTION_TITLE)))) {
                            uniqQURL.add(CharSeq.create(page.content(SegmentType.SECTION_TITLE)));
                            poolBuilder.accept(new QURLItem(page, query));
                            poolBuilder.advance();
                          }
                        });
                  }
                }
              });
      Pool<QURLItem> pool = poolBuilder.create();
      DataTools.writePoolTo(pool, Files.newBufferedWriter(poolPath));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
