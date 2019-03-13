package com.expleague.sensearch.snippet.experiments.pool;

import com.expleague.commons.random.FastRandom;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.data.tools.Pool;
import com.expleague.ml.meta.DataSetMeta;
import com.expleague.ml.meta.impl.JsonDataSetMeta;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.index.Index;
import com.expleague.ml.data.tools.Pool.Builder;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.experiments.Data;
import com.expleague.sensearch.snippet.features.AccumulatorFeatureSet;
import com.expleague.sensearch.snippet.features.QPASItem;
import com.expleague.sensearch.snippet.features.TargetFeatureSet;
import com.expleague.sensearch.snippet.passage.Passage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SnippetPoolBuilder {

  private final Index index;

  @Inject
  public SnippetPoolBuilder(Index index) {
    this.index = index;
  }

  public static void main(String[] args) throws IOException {
    Injector injector = Guice.createInjector(new AppModule());
    injector.getInstance(SnippetPoolBuilder.class).build(Paths.get("snippet.pool"));
  }

  private void build(Path path) {
    FastRandom rand = new FastRandom();
    DataSetMeta meta =
        new JsonDataSetMeta(
            "Google", "sensearch", new Date(), QPASItem.class, rand.nextBase64String(32));
    AccumulatorFeatureSet featureSet = new AccumulatorFeatureSet(index);
    TargetFeatureSet targetFeatureSet = new TargetFeatureSet(index);

    Builder<QPASItem> poolBuilder = Pool.builder(meta, featureSet, targetFeatureSet);

    AtomicInteger status = new AtomicInteger();
    try {
      byte[] jsonData = Files.readAllBytes(
          Paths.get("./src/main/java/com/expleague/sensearch/snippet/experiments/data.json"));
      ObjectMapper objectMapper = new ObjectMapper();
      Data[] datas = objectMapper.readValue(jsonData, Data[].class);

      Arrays.stream(datas)
          //.parallel()
          .forEach(data -> {
            if (status.get() % 10 == 0) {
              System.err.println(status + " datas completed");
            }

            status.incrementAndGet();
            Query query = BaseQuery.create(data.getQuery(), index);
            Optional<Page> page = index
                .allDocuments()
                .filter(x -> x.content(SegmentType.SECTION_TITLE).equals(data.getTitle()))
                .findFirst();
            if (page.isPresent()) {
              List<Passage> passages = page.get()
                  .sentences(SegmentType.SUB_BODY)
                  .map(x -> new Passage(x, index.parse(x).collect(Collectors.toList()), page.get()))
                  .collect(Collectors.toList());
              for (int i = 0; i < passages.size(); i++) {
                passages.get(i).setId(i);
              }

             // synchronized (poolBuilder) {
              System.out.println("--------------------");
              //System.out.println(data.getLong_answer());
              //System.out.println();
              //System.out.println(page.get().content(SegmentType.SUB_BODY));
              System.out.println(passages.size());
              for (Passage passage : passages) {
                  poolBuilder.accept(new QPASItem(query, passage));
                  poolBuilder.advance();
                }
              System.out.println("--------------------");
            //  }
            }
          });

      Pool<QPASItem> pool = poolBuilder.create();
      DataTools.writePoolTo(pool, Files.newBufferedWriter(path));
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}
