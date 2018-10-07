package components.crawler.document;

import java.util.List;

public interface CrawlerDocument {

    boolean checkWord(String word);

    List<Boolean> checkWords(List<String> words);

    List<CharSequence> returnSentences(String word);

    CharSequence returnContent();

    String getTitle();
}
