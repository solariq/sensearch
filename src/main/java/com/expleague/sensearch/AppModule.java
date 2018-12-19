package com.expleague.sensearch;

import com.expleague.sensearch.core.Annotations.MetricPath;
import com.expleague.sensearch.core.Annotations.PageSize;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.core.SearchPhaseFactory;
import com.expleague.sensearch.core.SenSeArchImpl;
import com.expleague.sensearch.donkey.IndexBuilder;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.metrics.RequestCrawler;
import com.expleague.sensearch.metrics.WebCrawler;
import com.expleague.sensearch.web.suggest.BigramsBasedSuggestor;
import com.expleague.sensearch.web.suggest.Suggestor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppModule extends AbstractModule {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  protected void configure() {
    try {
      Config config = objectMapper.readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);
      Lemmer lemmer = new Lemmer(config.getMyStem());

      bindConstant().annotatedWith(PageSize.class).to(config.getPageSize());

      bind(Path.class).annotatedWith(MetricPath.class).toInstance(config.getPathToMetrics());
      bind(Config.class).toInstance(config);
      bind(Lemmer.class).toInstance(lemmer);

      bind(Index.class).to(PlainIndex.class);
      bind(Suggestor.class).to(BigramsBasedSuggestor.class);
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
}
