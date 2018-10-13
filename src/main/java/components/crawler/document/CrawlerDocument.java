package components.crawler.document;

import java.util.List;

public interface CrawlerDocument {

    boolean checkWord(String word);

    CharSequence returnContent();

    String getTitle();

    Long getID();
}
