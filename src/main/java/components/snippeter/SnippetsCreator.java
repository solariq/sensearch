package components.snippeter;

import components.index.IndexedDocument;
import components.query.Query;
import components.snippeter.snippet.Cluster;
import components.snippeter.snippet.ClusteredSnippet;
import components.snippeter.snippet.Passage;
import components.snippeter.snippet.Snippet;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Maxim on 06.10.2018.
 * Email: alvinmax@mail.ru
 */
public class SnippetsCreator {

    private static final int PASSAGES_IN_CLUSTER = 4;

    private static final Pattern splitEnglish = Pattern.compile("(?<=[.!?]|[.!?]['\"])(?<!Mr\\.|Mrs\\.|Ms\\.|Jr\\.|Dr\\.|Prof\\.|Vol\\.|A\\.D\\.|B\\.C\\.|Sr\\.|T\\.V\\.A\\.)\\s+");
    private static final Pattern splitRussian = Pattern.compile("(?<=[.!?]|[.!?]['\"])(?<!\\(р\\.|\\(род\\.|[А-Я]\\.)");

    private static final Pattern splitPattern = splitRussian;

    public Snippet getSnippet(IndexedDocument document, Query query) {

        CharSequence title = document.getTitle();
        CharSequence content = document.getContent();

        List<Passage> passages = Arrays
                .stream(splitPattern.split(content))
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
