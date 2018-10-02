package simpleSearch.crawler.document;

import java.util.List;

public interface Document {

    boolean checkWord(String word);

    List<Boolean> checkWords(List<String> words);

    List<String> returnSentenses(String word);

    CharSequence returnContent();

    String getTitle();
}