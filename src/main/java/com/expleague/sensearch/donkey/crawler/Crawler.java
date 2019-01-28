package com.expleague.sensearch.donkey.crawler;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import java.io.IOException;
import java.util.stream.Stream;

public interface Crawler {

  Stream<CrawlerDocument> makeStream() throws IOException;
}
