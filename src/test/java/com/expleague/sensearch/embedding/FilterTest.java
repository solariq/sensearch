package com.expleague.sensearch.embedding;

import com.expleague.commons.text.lemmer.LemmaInfo;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.donkey.crawler.CrawlerXML;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndexBuilder;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FilterTest {

    private Index index;

    private static final Map<String, String[]> tests;

    static {
        Map<String, String[]> map = new HashMap<>();
        map.put("музыка России", new String[]{"музыка", "песня", "России", "русская", "русский"});
        tests = map;
    }

    @Before
    public void initIndex() throws Exception {
        byte[] jsonData = Files.readAllBytes(Paths.get("./config.json"));
        Config config = new ObjectMapper().readValue(jsonData, Config.class);
        index = new PlainIndexBuilder(config).buildIndex(new CrawlerXML(config).makeStream());
    }

    @Test
    public void filtrateTest() {
        for (Map.Entry<String, String[]> entry : tests.entrySet()) {
            Query query = new QueryStub(entry.getKey());
            Stream<Page> pageStream = index.fetchDocuments(query);
            Assert.assertTrue(pageStream.anyMatch(p -> Arrays.stream(entry.getValue()).anyMatch(w -> p.title().toString().contains(w))));
        }
    }

    private static class QueryStub implements Query {
        private List<Term> terms;
        QueryStub(String query) {
            terms = new LinkedList<>();
            for (String w : query.split("[^А-ЯЁа-яёA-Za-z0-9]")) {
                terms.add(new TermStub(w));
            }
        }

        @Override
        public List<Term> getTerms() {
            return terms;
        }
    }

    private static class TermStub implements Term {
        private String rawTerm;

        TermStub(String term) {
            rawTerm = term;
        }

        @Override
        public CharSequence getRaw() {
            return rawTerm;
        }

        @Override
        public CharSequence getNormalized() {
            return null;
        }

        @Override
        public LemmaInfo getLemma() {
            return null;
        }
    }
}