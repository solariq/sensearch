package com.expleague.sensearch;

import com.expleague.commons.math.Trans;
import com.expleague.commons.math.vectors.SingleValueVec;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.util.Pair;
import com.expleague.ml.data.tools.DataTools;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.core.Annotations.DataZipPath;
import com.expleague.sensearch.core.Annotations.EmbeddingLshTablesDb;
import com.expleague.sensearch.core.Annotations.EmbeddingVecsDb;
import com.expleague.sensearch.core.Annotations.EmbeddingVectorsPath;
import com.expleague.sensearch.core.Annotations.FilterMaxItems;
import com.expleague.sensearch.core.Annotations.IndexRoot;
import com.expleague.sensearch.core.Annotations.MetricPath;
import com.expleague.sensearch.core.Annotations.PageSize;
import com.expleague.sensearch.core.Annotations.RankFilterModel;
import com.expleague.sensearch.core.Annotations.RankModel;
import com.expleague.sensearch.core.Annotations.SnippetModel;
import com.expleague.sensearch.core.Annotations.UseLshFlag;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.core.SearchPhaseFactory;
import com.expleague.sensearch.core.SenSeArchImpl;
import com.expleague.sensearch.core.Stemmer;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.filter.Filter;
import com.expleague.sensearch.filter.FilterImpl;
import com.expleague.sensearch.index.Embedding;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.EmbeddingImpl;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.metrics.RequestCrawler;
import com.expleague.sensearch.metrics.WebCrawler;
import com.expleague.sensearch.web.suggest.OneWordSuggestor;
import com.expleague.sensearch.web.suggest.Suggestor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import org.apache.log4j.Logger;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

public class AppModule extends AbstractModule {

  private static final Logger LOG = Logger.getLogger(AppModule.class.getName());

  private static final long CACHE_SIZE = 32 * (1 << 20);
  private static final Options DB_OPTIONS = new Options().cacheSize(CACHE_SIZE);

  private final Config config;

  public AppModule() throws IOException {
    this.config =
        new ObjectMapper().readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);
  }

  public AppModule(Config config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    // binding constants
    bindConstant().annotatedWith(FilterMaxItems.class).to(config.maxFilterItems());
    bindConstant().annotatedWith(PageSize.class).to(config.getPageSize());

    bind(Path.class).annotatedWith(MetricPath.class).toInstance(config.getPathToMetrics());
    bind(Path.class)
        .annotatedWith(EmbeddingVectorsPath.class)
        .toInstance(config.getEmbeddingVectors());
    bind(Path.class).annotatedWith(DataZipPath.class).toInstance(config.getPathToZIP());
    bind(Path.class).annotatedWith(IndexRoot.class).toInstance(config.getIndexRoot());
    bindConstant().annotatedWith(UseLshFlag.class).to(config.getLshNearestFlag());

    Lemmer lemmer = new Lemmer(new Stemmer());
    bind(Lemmer.class).toInstance(lemmer);

    bind(Crawler.class).to(CrawlerXML.class);
    bind(Embedding.class).to(EmbeddingImpl.class).in(Singleton.class);
    bind(Filter.class).to(FilterImpl.class);
    bind(Index.class).to(PlainIndex.class).in(Singleton.class);
    bind(IndexBuilder.class).to(PlainIndexBuilder.class);
    bind(Suggestor.class).to(OneWordSuggestor.class).in(Singleton.class);
    bind(SenSeArch.class).to(SenSeArchImpl.class);
    bind(WebCrawler.class).to(RequestCrawler.class);

    install(new FactoryModuleBuilder().build(SearchPhaseFactory.class));
  }

  @Provides
  @Singleton
  @EmbeddingVecsDb
  DB getEmbeddingDb() throws IOException {
    return JniDBFactory.factory.open(
        config
            .getIndexRoot()
            .resolve(PlainIndexBuilder.EMBEDDING_ROOT)
            .resolve(PlainIndexBuilder.VECS_ROOT)
            .toFile(),
        DB_OPTIONS);
  }

  @Provides
  @Singleton
  @EmbeddingLshTablesDb
  DB getLshDb() throws IOException {
    return JniDBFactory.factory.open(
        config
            .getIndexRoot()
            .resolve(PlainIndexBuilder.EMBEDDING_ROOT)
            .resolve(PlainIndexBuilder.LSH_ROOT)
            .toFile(),
        DB_OPTIONS);
  }

  private Pair<Function, FeatureMeta[]> getModelStub() {
    return new Pair<>(
        new Trans.Stub() {

          @Override
          public int xdim() {
            return 0;
          }

          @Override
          public int ydim() {
            return 0;
          }

          @Override
          public Vec trans(Vec arg) {
            return new SingleValueVec(1);
          }
        },
        new FeatureMeta[0]);
  }

  @Provides
  @Singleton
  @RankModel
  Pair<Function, FeatureMeta[]> getRankModel() throws IOException {
    if (Files.exists(config.getModelPath())) {
      return DataTools.readModel(
          new InputStreamReader(
              Files.newInputStream(config.getModelPath()), StandardCharsets.UTF_8));
    }

    LOG.warn(
        "Rank model can not be found at path ["
            + config.getModelPath()
            + "], using empty model instead");
    return getModelStub();
  }

  @Provides
  @Singleton
  @RankFilterModel
  Pair<Function, FeatureMeta[]> getRankFilterModel() throws IOException {
    if (Files.exists(config.getFilterModelPath())) {
      return DataTools.readModel(
          new InputStreamReader(
              Files.newInputStream(config.getFilterModelPath()), StandardCharsets.UTF_8));
    }

    LOG.warn(
        "Filter model can not be found at path ["
            + config.getFilterModelPath()
            + "], using empty model instead");
    return getModelStub();
  }

  @Provides
  @Singleton
  @SnippetModel
  Pair<Function, FeatureMeta[]> getSnippetModel() throws IOException {
    if (Files.exists(config.getSnippetModelPath())) {
      return DataTools.readModel(
          new InputStreamReader(
              Files.newInputStream(config.getSnippetModelPath()), StandardCharsets.UTF_8));
    }

    LOG.warn(
        "Filter model can not be found at path ["
            + config.getSnippetModelPath()
            + "], using empty model instead");
    return getModelStub();
  }


}
