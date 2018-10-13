package components.crawler.document;

public interface CrawlerDocument {

    boolean checkWord(String word);

    CharSequence returnContent();

    String getTitle();

    Long getID();
}
