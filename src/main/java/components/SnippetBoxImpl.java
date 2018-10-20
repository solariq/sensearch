package components;

import components.crawler.CrawlerXML;
import components.index.Index;
import components.index.IndexedDocument;
import components.index.plain.PlainIndexBuilder;
import components.query.BaseQuery;
import components.query.Query;
import components.searcher.FuzzySearcher;
import components.searcher.Searcher;
import components.snippeter.SnippetsCreator;
import components.snippeter.snippet.Snippet;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SnippetBoxImpl implements SnippetBox {
    final private SnippetsCreator snippetsCreator = new SnippetsCreator();
    private Index index;
    private Searcher searcher;

    public SnippetBoxImpl (Path path) {
        try {
            Path pathInd = path.getParent().resolve("IndexTmp");
            Files.createDirectories(pathInd);
            FileUtils.deleteDirectory(pathInd.toFile());
            index = new PlainIndexBuilder(pathInd)
                        .buildIndex(new CrawlerXML(path).makeStream());
            searcher = new FuzzySearcher(index, 4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Query query;
    private List<IndexedDocument> docList = new ArrayList<>();
    private ArrayList<Snippet> snippets = new ArrayList<>();

    @Override
    public int size() {
        return docList.size();
    }

    @Override
    public boolean makeQuery(CharSequence s) {
        docList.clear();
        snippets.clear();
        query = new BaseQuery(s);
        docList = searcher.getRankedDocuments(query);
        for (IndexedDocument doc : docList) {
            snippets.add(snippetsCreator.getSnippet(doc, query));
        }
        return true;
    }

    @Override
    public Snippet getSnippet(int idx) {
        return snippets.get(idx);
    }
}
