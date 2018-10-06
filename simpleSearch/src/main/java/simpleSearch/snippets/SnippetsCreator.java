package simpleSearch.snippets;

import simpleSearch.baseSearch.Query;
import simpleSearch.crawler.Crawler;
import simpleSearch.crawler.document.MyDocument;
import simpleSearch.snippets.snippet.Snippet;

import java.util.List;

/**
 * Created by Maxim on 06.10.2018.
 * Email: alvinmax@mail.ru
 */
public class SnippetsCreator {

    private static final int COUNT = 3;

    Query query;
    long[] documents;
    List<Snippet> results;

    SnippetsCreator(Query query, long[] documents) {
        this.query = query;
        this.documents = documents;
        generateSnippets();
    }

    private void generateSnippets() {
        Crawler crawler = /*jdem*/;
        List<String> words = query.getWords();

        long best = 0;
        Snippet result = null;

        for (int i = 0; i < documents.length; ++i) {
            MyDocument document = crawler.getDocument();
            List<String> sentences = document.returnSentenses();
            for (String sentence : sentences) {
                long count = words.stream().filter(word -> Utils.contains(sentence, word)).count();
                if (count > best) {
                    best = count;
                    result = new Snippet(document.getTitle(), sentence);
                }
            }
        }

        results.add(result);
    }

    public List<Snippet> getResults() {
        return results;
    }
}
