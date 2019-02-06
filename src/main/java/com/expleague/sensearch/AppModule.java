package com.expleague.sensearch;

import com.expleague.sensearch.core.Annotations.EmbeddingLshTablesDb;
import com.expleague.sensearch.core.Annotations.EmbeddingPath;
import com.expleague.sensearch.core.Annotations.EmbeddingVecsDb;
import com.expleague.sensearch.core.Annotations.FilterMaxItems;
import com.expleague.sensearch.core.Annotations.MetricPath;
import com.expleague.sensearch.core.Annotations.PageSize;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.core.SearchPhaseFactory;
import com.expleague.sensearch.core.SenSeArchImpl;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.index.Embedding;
import com.expleague.sensearch.index.Filter;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.EmbeddingImpl;
import com.expleague.sensearch.index.plain.FilterImpl;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.metrics.RequestCrawler;
import com.expleague.sensearch.metrics.WebCrawler;
import com.expleague.sensearch.web.suggest.ProbabilisticSuggestor;
import com.expleague.sensearch.web.suggest.Suggestor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

public class AppModule extends AbstractModule {

  private static final long CACHE_SIZE = 16 * (1 << 20);
  private static final Options DB_OPTIONS = new Options().cacheSize(CACHE_SIZE);

  private final ObjectMapper objectMapper = new ObjectMapper();
  private Path embeddingPath;

  @Override
  protected void configure() {
    try {
      Config config = objectMapper.readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);
      embeddingPath = config.getTemporaryIndex().resolve(PlainIndexBuilder.EMBEDDING_ROOT);
      Lemmer lemmer = new Lemmer(config.getMyStem());

      bind(Embedding.class).to(EmbeddingImpl.class);
      bind(Filter.class).to(FilterImpl.class);

      bindConstant().annotatedWith(FilterMaxItems.class).to(config.maxFilterItems());
      bindConstant().annotatedWith(PageSize.class).to(config.getPageSize());
      bind(Path.class).annotatedWith(MetricPath.class).toInstance(config.getPathToMetrics());

      bind(Path.class).annotatedWith(EmbeddingPath.class).toInstance(embeddingPath);

      bind(Config.class).toInstance(config);
      bind(Lemmer.class).toInstance(lemmer);

      bind(Index.class).to(PlainIndex.class);
      // bind(Suggestor.class).to(BigramsBasedSuggestor.class);
      bind(Suggestor.class).to(ProbabilisticSuggestor.class);
      bind(SenSeArch.class).to(SenSeArchImpl.class);
      bind(IndexBuilder.class).to(PlainIndexBuilder.class);
      bind(Crawler.class).to(CrawlerXML.class);
      bind(WebCrawler.class).to(RequestCrawler.class);

      install(new FactoryModuleBuilder().build(SearchPhaseFactory.class));
      //      bind(SearchPhaseFactory.class)
      //          .toProvider(FactoryProvider.newFactory(SearchPhaseFactory.class,
      // QueryPhase.class));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Provides
  @Singleton
  @EmbeddingVecsDb
  DB getEmbeddingDb() throws IOException {
    return JniDBFactory.factory.open(
        embeddingPath.resolve(PlainIndexBuilder.VECS_ROOT).toFile(), DB_OPTIONS);
  }

  @Provides
  @Singleton
  @EmbeddingLshTablesDb
  DB getLshDb() throws IOException {
    return JniDBFactory.factory.open(
        embeddingPath.resolve(PlainIndexBuilder.LSH_ROOT).toFile(), DB_OPTIONS);
  }

}
