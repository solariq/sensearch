package components.crawler;

import components.crawler.document.CrawlerDocument;
import java.io.FileNotFoundException;
import java.util.stream.Stream;

public interface Crawler {

  Stream<CrawlerDocument> makeStream() throws FileNotFoundException;

}
