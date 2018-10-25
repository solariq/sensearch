package components;

import com.fasterxml.jackson.databind.ObjectMapper;
import components.crawler.Crawler;
import components.crawler.CrawlerXML;
import components.index.Index;
import components.index.plain.PlainIndexBuilder;
import components.searcher.FuzzySearcher;
import components.searcher.Searcher;
import components.suggestor.BigramsBasedSuggestor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Builder {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static Index index;
    private static Searcher searcher;
    private static PageLoadHandler pageLoadHandler;
    private static SnippetBox snippetBox;
    private static Crawler crawler;
    private static BigramsBasedSuggestor bigramsBasedSuggestor;

    private static void init() {
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get("./paths.json"));
            objectMapper.readValue(jsonData, Constants.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void build() throws IOException {
        init();

        crawler = new CrawlerXML(Constants.getPathToZIP());
        index = new PlainIndexBuilder(Constants.getTemporaryIndex()).buildIndex(crawler.makeStream());
        searcher = new FuzzySearcher(index, 4);
        snippetBox = new SnippetBoxImpl(searcher);
        bigramsBasedSuggestor = new BigramsBasedSuggestor(Constants.getBigramsFileName());
        pageLoadHandler = new PageLoadHandler(snippetBox, bigramsBasedSuggestor);
    }


    static PageLoadHandler getPageLoadHendler() {
        return pageLoadHandler;
    }

    static SnippetBox getSnippetBox() {
        return snippetBox;
    }

    static Index getIndex() {
        return index;
    }

    static Searcher getSearcher() {
        return searcher;
    }

    static Crawler getCrawler() {
        return crawler;
    }
}
