package components.crawler;

import components.crawler.document.CrawlerDocument;

import java.util.stream.Stream;

public interface Crawler {

    Stream<CrawlerDocument> makeStream();

}
