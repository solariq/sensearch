package com.expleague.sensearch.snippet.experiments.pool;

import com.expleague.commons.func.Functions;
import com.expleague.commons.math.Trans;
import com.expleague.commons.random.FastRandom;
import com.expleague.commons.util.Pair;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.data.tools.Pool;
import com.expleague.ml.meta.DataSetMeta;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.impl.JsonDataSetMeta;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Annotations;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainPage;
import com.expleague.sensearch.miner.pool.builders.PoolBuilder;
import com.expleague.sensearch.miner.pool.builders.RankingPoolBuilder;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.features.AccumulatorFeatureSet;
import com.expleague.sensearch.snippet.features.QPASItem;
import com.expleague.sensearch.snippet.features.TargetFeatureSet;
import com.expleague.sensearch.snippet.passage.Passage;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.expleague.sensearch.core.Annotations.SnippetModel;
import com.google.inject.Injector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SnippetPoolBuilder extends PoolBuilder<QueryAndPassages> {
    private static final Logger LOG = Logger.getLogger(SnippetPoolBuilder.class.getName());

    private final Index index;
    private final Trans model;
    private final FastRandom random = new FastRandom();

    @Inject
    public SnippetPoolBuilder(Index index, @SnippetModel Pair<Function, FeatureMeta[]> snippetModel) {
        this.index = index;
        this.model = (Trans) snippetModel.getFirst();
    }

    public static void main(String[] args) throws IOException {
        ConfigImpl config = new ConfigImpl();
        config.setTemporaryIndex(args[0]);
        if (args.length > 1) {
            config.setSnippetModelPath(args[1]);
        }
        Injector injector = Guice.createInjector(new AppModule(config));
        injector.getInstance(SnippetPoolBuilder.class).build(Paths.get("./PoolData/snippet/"), 0);
    }

    private void build(Path dataPath, int iteration) {
        LOG.info("SnippetPool build started");
        long startTime = System.nanoTime();
        FastRandom rand = new FastRandom();
        DataSetMeta meta = new JsonDataSetMeta("Google", "sensearch", new Date(), QPASItem.class, rand.nextBase64String(32));
        AccumulatorFeatureSet features = new AccumulatorFeatureSet(index);
        TargetFeatureSet targetFeatures = new TargetFeatureSet();

        Pool.Builder<QPASItem> poolBuilder = Pool.builder(meta, features, targetFeatures);

        AtomicInteger status = new AtomicInteger(0);

        QueryAndPassages[] examples = readData(QueryAndPassages.class, iteration, dataPath);
        Arrays.stream(examples)
                .parallel()
                .forEach(example -> {
                    if (status.get() % 10000 == 0) {
                        System.err.println(status.get() + " examples are completed.");
                    }
                    status.incrementAndGet();

                    Page page = index.page(example.uri());
                    if (page != PlainPage.EMPTY_PAGE) {
                        Query query = BaseQuery.create(example.query(), index);
                        List<QueryAndPassages.PassageAndWeight> answers = Arrays.asList(example.answers());
                        if (answers.isEmpty()) return;

                        answers.forEach(paw -> {
                            Passage passage = new Passage(index.parse(paw.passage()).collect(Collectors.toList()), page);
                            synchronized (poolBuilder) {
                                poolBuilder.accept(new QPASItem(query, passage));
                                poolBuilder
                                        .features()
                                        .map(Functions.cast(TargetFeatureSet.class))
                                        .filter(Objects::nonNull)
                                        .forEach(fs -> fs.withWeight(paw.weight()));
                                poolBuilder.advance();
                            }
                        });
                    }
                });


        Pool<QPASItem> pool = poolBuilder.create();
        try {
            DataTools.writePoolTo(pool, Files.newBufferedWriter(dataPath.resolve("snippet.pool")));
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        LOG.info(
                String.format(
                        "SnippetPool built in %.3f seconds", (System.nanoTime() - startTime) / 1e9));
    }
}