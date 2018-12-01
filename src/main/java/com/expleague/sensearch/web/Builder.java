package com.expleague.sensearch.web;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.core.SenSeArchImpl;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.metrics.Metric;
import com.expleague.sensearch.metrics.RequestCrawler;
import com.expleague.sensearch.web.suggest.BigramsBasedSuggestor;
import com.expleague.sensearch.web.suggest.Suggestor;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;

@Singleton
public class Builder {

  private Index index;
  private SenSeArch searcher;
  private Crawler crawler;
  private Suggestor suggestor;
  private Config config;
  private Lemmer lemmer;
  private Metric metric;

  @Inject
  public Builder(Config config) {
    this.config = config;
  }

  public Config build() throws IOException, XMLStreamException {
    crawler = new CrawlerXML(config);
    final Path myStem = config.getMyStem();
    lemmer = new Lemmer(myStem);
    if (config.getBuildIndexFlag()) {
      new PlainIndexBuilder().buildIndex(crawler, config, lemmer);
    }
    index = new PlainIndex(config);

    lemmer = new Lemmer(myStem);
    suggestor = new BigramsBasedSuggestor(index);
    //suggestor = new ProbabilisticSuggestor(crawler, index);

    suggestor = new BigramsBasedSuggestor(index);

    searcher = new SenSeArchImpl(this);
    metric = new Metric(new RequestCrawler(index), config.getPathToMetrics());
    return config;
  }

  public Index getIndex() {
    return index;
  }

  public SenSeArch getSearcher() {
    return searcher;
  }

  Crawler getCrawler() {
    return crawler;
  }

  Suggestor getSuggestor() {
    return suggestor;
  }

  public int pageSize() {
    return 10;
  }

  public int windowSize() {
    return 4;
  }

  public Lemmer getLemmer() {
    return lemmer;
  }

  public Metric metric() {
    return metric;
  }
}
