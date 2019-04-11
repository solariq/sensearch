// package com.expleague.sensearch.embedding;
//
// import com.expleague.commons.text.lemmer.LemmaInfo;
// import com.expleague.sensearch.ConfigImpl;
// import com.expleague.sensearch.Page;
// import com.expleague.sensearch.experiments.wiki.CrawlerWiki;
// import com.expleague.sensearch.donkey.plain.PlainIndexBuilder;
// import com.expleague.sensearch.index.Index;
// import com.expleague.sensearch.query.Query;
// import com.expleague.sensearch.query.term.Term;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import java.nio.file.Files;
// import java.nio.file.Paths;
// import java.util.Arrays;
// import java.util.HashMap;
// import java.util.LinkedList;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Stream;
// import org.junit.Assert;
// import org.junit.Before;
// import org.junit.Test;
//
// public class FilterTest {
//
//  private static final Map<String, String[]> tests;
//
//  static {
//    Map<String, String[]> map = new HashMap<>();
//    map.put("музыка России", new String[]{"музыка", "песня", "России", "русская", "русский"});
//    tests = map;
//  }
//
//  private Index index;
//
//  @Before
//  public void initIndex() throws Exception {
//    byte[] jsonData = Files.readAllBytes(Paths.get("./config.json"));
//    ConfigImpl config = new ObjectMapper().readValue(jsonData, ConfigImpl.class);
//    index = new PlainIndexBuilder(config).buildIndex(new CrawlerWiki(config).makeStream());
//  }
//
//  @Test
//  public void filtrateTest() {
//    for (Map.Entry<String, String[]> entry : tests.entrySet()) {
//      Query query = new QueryStub(entry.getKey());
//      Stream<Page> pageStream = index.fetchDocuments(query);
//      Assert.assertTrue(pageStream.anyMatch(
//          p -> Arrays.stream(entry.getValue()).anyMatch(w -> p.titles().toString().contains(w))));
//    }
//  }
//
//  private static class QueryStub implements Query {
//
//    private List<Term> terms;
//
//    QueryStub(String query) {
//      terms = new LinkedList<>();
//      for (String w : query.split("[^А-ЯЁа-яёA-Za-z0-9]")) {
//        terms.add(new TermStub(w));
//      }
//    }
//
//    @Override
//    public List<Term> getTerms() {
//      return terms;
//    }
//  }
//
//  private static class TermStub implements Term {
//
//    private String rawTerm;
//
//    TermStub(String term) {
//      rawTerm = term;
//    }
//
//    @Override
//    public CharSequence getRaw() {
//      return rawTerm;
//    }
//
//    @Override
//    public CharSequence getNormalized() {
//      return null;
//    }
//
//    @Override
//    public LemmaInfo lemma() {
//      return null;
//    }
//  }
// }
