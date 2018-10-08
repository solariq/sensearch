package components.snippeter;

import java.util.LinkedList;
import components.crawler.Crawler;
import components.crawler.document.CrawlerDocument;
import components.query.term.Term;
import components.snippeter.snippet.Snippet;
import components.query.Query;

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
//        generateSnippets();
    }

    private void generateSnippets(Crawler crawler) {
        List<Term> words = query.getTerms();

        long best = 0;
        Snippet result = null;

        for (int i = 0; i < documents.length; ++i) {
            CrawlerDocument document = crawler.getDocument(0L);
            List<String> sentences = new LinkedList<>();//document.returnSentenses();
            for (String sentence : sentences) {
                long count = words.stream().filter(word -> Utils.contains(sentence, word.getRaw().toString())).count();
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
