package com.expleague.sensearch.web;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.core.SenSeArchImpl;
import com.expleague.sensearch.index.statistics.Stats;
import com.expleague.sensearch.web.suggest.Suggestor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndexBuilder;
import com.expleague.sensearch.web.suggest.BigramsBasedSuggestor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLStreamException;

public class Builder {

  private ObjectMapper objectMapper = new ObjectMapper();
  private Index index;
  private SenSeArch searcher;
  private Crawler crawler;
  private BigramsBasedSuggestor bigramsBasedSuggestor;
  private Stats statistics;
  private Path configPath;
  private Config config;

  public Builder(Path pathToConfig) {
    configPath = pathToConfig;
    init();
  }

  private void init() {
    try {
      byte[] jsonData = Files.readAllBytes(configPath);
      config = objectMapper.readValue(jsonData, Config.class);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  Config build() throws IOException, XMLStreamException {
    crawler = new CrawlerXML(config);
    index = new PlainIndexBuilder(config).buildIndex(crawler.makeStream());
    bigramsBasedSuggestor = new BigramsBasedSuggestor(config);
    searcher = new SenSeArchImpl(this);
    return config;
  }

  public Index getIndex() {
    return index;
  }

  SenSeArch getSearcher() {
    return searcher;
  }

  Crawler getCrawler() {
    return crawler;
  }

  public Suggestor getSuggestor() {
    return bigramsBasedSuggestor;
  }

  public int pageSize() {
    return 10;
  }

  public int windowSize() {
    return 4;
  }

  Stats getStatistics() {
    return statistics;
  }
}
