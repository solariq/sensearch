package com.expleague.sensearch.embedding;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.index.plain.PlainIndexBuilder;
import com.expleague.sensearch.query.BaseQuery;
import com.expleague.sensearch.query.Query;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

public class FilterTest {

    private Index index;

    private static final Map<String, String[]> tests;

    static {
        Map<String, String[]> map = new HashMap<>();
        map.put("музыка России", new String[]{"музыка", "песня", "Россия", "русская", "русский"});
        tests = map;
    }

    @Before
    public void initIndex() throws Exception {
        byte[] jsonData = Files.readAllBytes(Paths.get("./paths.json"));
        Config config = new ObjectMapper().readValue(jsonData, Config.class);
        index = new PlainIndexBuilder(config).buildIndex(new CrawlerXML(config).makeStream());
    }

    @Test
    public void filtrateTest() {
        for (Map.Entry<String, String[]> entry : tests.entrySet()) {
            Query query = new BaseQuery(entry.getKey());
            Stream<IndexedPage> pageStream = index.fetchDocuments(query);
            pageStream.forEach(indexedPage -> System.out.println(indexedPage.title()));
        }
    }
}