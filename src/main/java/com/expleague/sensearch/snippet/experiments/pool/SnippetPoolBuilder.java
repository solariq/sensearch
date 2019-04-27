package com.expleague.sensearch.snippet.experiments.pool;

import com.expleague.commons.math.Trans;
import com.expleague.commons.random.FastRandom;
import com.expleague.commons.util.Pair;
import com.expleague.ml.data.tools.Pool;
import com.expleague.ml.meta.DataSetMeta;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.impl.JsonDataSetMeta;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.core.Annotations;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.miner.pool.builders.PoolBuilder;
import com.expleague.sensearch.miner.pool.builders.RankingPoolBuilder;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.features.AccumulatorFeatureSet;
import com.expleague.sensearch.snippet.features.QPASItem;
import com.expleague.sensearch.snippet.features.TargetFeatureSet;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.expleague.sensearch.core.Annotations.SnippetModel;
import com.google.inject.Injector;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Logger;

public class SnippetPoolBuilder extends PoolBuilder<QueryAndPassages> {
    private static final Logger LOG = Logger.getLogger(SnippetPoolBuilder.class.getName());

    private final Index index;
    private final Trans model;


    @Inject
    public SnippetPoolBuilder(Index index, @SnippetModel Pair<Function, FeatureMeta[]> snippetModel) {
        this.index = index;
        this.model = (Trans) snippetModel.getFirst();
    }

    public static void main(String[] args) throws IOException {
        Injector injector = Guice.createInjector(new AppModule());
        injector.getInstance(SnippetPoolBuilder.class).build(Paths.get("./PoolData/snippet/"), 0);
    }

    private void build(Path dataPath, int iteration) {
        LOG.info("SnippetPool build started");
        long startTime = System.nanoTime();
        FastRandom rand = new FastRandom();
        DataSetMeta meta = new JsonDataSetMeta("Google", "sensearch", new Date(), QPASItem.class, rand.nextBase64String(32));
        AccumulatorFeatureSet features = new AccumulatorFeatureSet(index);
        TargetFeatureSet targetFeatures = new TargetFeatureSet();

        FeatureMeta[] metas = metaData(features, targetFeatures);
        Pool.Builder<QPASItem> poolBuilder = Pool.builder(meta, features, targetFeatures);

        AtomicInteger status = new AtomicInteger(0);
        AtomicInteger added = new AtomicInteger(0);

        QueryAndPassages[] positiveExamples = readData(QueryAndPassages.class, iteration, dataPath);
        List<QueryAndPassages> newData = Collections.synchronizedList(new ArrayList<>());
        Arrays.stream(positiveExamples)
                .parallel()
                .forEach(positiveExample -> {
                    final int[] count = {0};
                    if (status.get() % 10000 == 0) {
                        System.err.println(status.get() + " examples are completed.");
                    }
                    status.incrementAndGet();

                    Query query = BaseQuery.create(positiveExample.query(), index);
                    List<QueryAndPassages.PassageAndWeight> res = Arrays.asList(positiveExample.answers());

//                    res.
                    //TODO
                });
    }
}
