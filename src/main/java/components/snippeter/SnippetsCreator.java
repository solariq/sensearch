package components.snippeter;

import java.util.Arrays;
import java.util.List;
import components.crawler.document.CrawlerDocument;
import components.query.Query;
import components.snippeter.snippet.Cluster;
import components.snippeter.snippet.Passage;
import components.snippeter.snippet.ClusteredSnippet;
import components.snippeter.snippet.Snippet;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Maxim on 06.10.2018.
 * Email: alvinmax@mail.ru
 */
public class SnippetsCreator {

    private static final int PASSAGES_IN_CLUSTER = 4;
    private static final Pattern splitPattern = Pattern.compile("(?<=[.!?])");

    public Snippet getSnippet(CrawlerDocument document, Query query) {

/*
        CharSequence test = "Emperor Akbar was in the habit of putting riddles and puzzles to his courtiers. He often asked questions which were strange and witty. It took much wisdom to answer these questions.\n" +
                "Once he asked a very strange question. The courtiers were dumb folded by his question.\n" +
                "Akbar glanced at his courtiers. As he looked, one by one the heads began to hang low in search of an answer. It was at this moment that Birbal entered the courtyard. Birbal who knew the nature of the emperor quickly grasped the situation and asked, \"May I know the question so that I can try for an answer\".\n" +
                "Akbar said, \"How many crows are there in this city?\"\n" +
                "Without even a moment's thought, Birbal replied \"There are fifty thousand five hundred and eighty nine crows, my lord\". \n" +
                "\"How can you be so sure?\" asked Akbar. \n" +
                "Birbal said, \"Make you men count, My lord. If you find more crows it means some have come to visit their relatives here. If you find less number of crows it means some have gone to visit their relatives elsewhere\". \n" +
                "Akbar was pleased very much by Birbal's wit.\n\n\n";

        System.out.print(test);
*/

        CharSequence title = document.getTitle();
        CharSequence content = document.returnContent();

        List<Passage> passages = Arrays
                .asList(splitPattern.split(content))
                .stream()
                .map(x -> new Passage(x, query))
                .collect(Collectors.toList());

        Cluster best = null;
        int n = Math.min(PASSAGES_IN_CLUSTER, passages.size());
        for (int i = 0; i < passages.size() - n + 1; ++i) {
            Cluster cluster = new Cluster(passages.subList(i, i + n));
            if (best == null || best.getRating() < cluster.getRating()) {
                best = cluster;
            }
        }

        return new ClusteredSnippet(title, best);
    }

}
