package components.crawler.document;

public interface CrawlerDocument {

  CharSequence getContent();

  String getTitle();

  Long getID();
}
