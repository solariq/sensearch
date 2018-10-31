package com.expleague.sensearch.web;

import com.expleague.sensearch.Constants;
import com.expleague.sensearch.FuzzySearcher;
import com.expleague.sensearch.SenSeArch;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndexBuilder;
import com.expleague.sensearch.web.PageLoadHandler;
import com.expleague.sensearch.snippet.SnippetBox;
import com.expleague.sensearch.snippet.SnippetBoxImpl;
import com.expleague.sensearch.web.suggest.BigramsBasedSuggestor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.xml.stream.XMLStreamException;

public class Builder {

  private static ObjectMapper objectMapper = new ObjectMapper();

  private static Index index;
  private static SenSeArch searcher;
  private static PageLoadHandler pageLoadHandler;
  private static SnippetBox snippetBox;
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
    searcher = new FuzzySearcher(index, 4);
    snippetBox = new SnippetBoxImpl(searcher);
    bigramsBasedSuggestor = new BigramsBasedSuggestor(Constants.getBigramsFileName());
    pageLoadHandler = new PageLoadHandler(snippetBox, bigramsBasedSuggestor);
    //statistics = Stats.readStatsFromFile(Constants.getStatisticsFileName());
  }


  static PageLoadHandler getPageLoadHandler() {
    return pageLoadHandler;
  }

  static SnippetBox getSnippetBox() {
    return snippetBox;
  }

  static Index getIndex() {
    return index;
  }

  static SenSeArch getSearcher() {
    return searcher;
  }

  static Crawler getCrawler() {
    return crawler;
  }

  static Stats getStatistics() {
	  return statistics;
  }
}
