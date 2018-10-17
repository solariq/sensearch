package components.crawler.document;

public interface CrawlerDocument {

    CharSequence returnContent();

    String getTitle();

    Long getID();
}
