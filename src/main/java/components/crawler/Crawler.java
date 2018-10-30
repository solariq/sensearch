package components.crawler;

import components.crawler.document.CrawlerDocument;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;

public interface Crawler {

  Stream<CrawlerDocument> makeStream() throws IOException, XMLStreamException;

}
