package com.expleague.sensearch.web;

import com.expleague.sensearch.Constants;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.core.SenSeArchImpl;
import com.expleague.sensearch.index.statistics.Stats;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndexBuilder;
import com.expleague.sensearch.web.suggest.BigramsBasedSuggestor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.stream.XMLStreamException;

public class Builder {

  private static ObjectMapper objectMapper = new ObjectMapper();

  private static Index index;
  private static SenSeArch searcher = new SenSeArchImpl();
  private static PageLoadHandler pageLoadHandler;
  private static Crawler crawler;
  private static BigramsBasedSuggestor bigramsBasedSuggestor;
  private static Stats statistics;

  private static void init() {
    try {
      byte[] jsonData = Files.readAllBytes(Paths.get("./paths.json"));
      objectMapper.readValue(jsonData, Constants.class);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static void build() throws IOException, XMLStreamException {
    init();

    crawler = new CrawlerXML(Constants.getPathToZIP());
    index = new PlainIndexBuilder(Constants.getTemporaryIndex()).buildIndex(crawler.makeStream());
    bigramsBasedSuggestor = new BigramsBasedSuggestor(Constants.getBigramsFileName());
    pageLoadHandler = new PageLoadHandler(searcher, bigramsBasedSuggestor);
  }


  static PageLoadHandler getPageLoadHandler() {
    return pageLoadHandler;
  }

  public static Index getIndex() {
    return index;
  }

  static SenSeArch getSearcher() {
    return searcher;
  }

  static Crawler getCrawler() {
    return crawler;
  }

  public int pageSize() {
    return 10;
  }

  public int windowSize() {
    return 4;
  }

  static Stats getStatistics() {
    return statistics;
  }
}
