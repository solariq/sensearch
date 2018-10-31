package com.expleague.sensearch.searcher;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by sandulmv on 06.10.18.
 */
public class ComponentsInteractionTest {
//  private static final String[] TEST_DOCUMENTS = new String[]{
//      "Information Retrieval and Web Search".toLowerCase(),
//      "Search Engine Ranking".toLowerCase(),
//      "Web Search Course".toLowerCase()
//  };
//
//  @Test
//  public void test() {
//    SenSeArch wordFilterSearcher = new WordFilterSearcher(new SimpleIndex());
//    Query query = new SimpleQuery("web search");
//    List<Page> foundDocuments = wordFilterSearcher.search(query).collect(Collectors.toList());
//    System.out.print(foundDocuments);
//    Assert.assertEquals(foundDocuments.size(), 3);
//  }
//
//  private static class MockPage implements IndexedPage {
//    private CharSequence documentContent;
//    private long documentId;
//
//    public MockPage(long documentId, CharSequence documentContent) {
//      this.documentContent = documentContent;
//      this.documentId = documentId;
//    }
//
//    @Override
//    public CharSequence text() {
//      return documentContent;
//    }
//
//    @Override
//    public long id() {
//      return documentId;
//    }
//
//    @Override
//    public URI reference() {
//      throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public CharSequence title() {
//      throw new UnsupportedOperationException("Class does not implement 'getTitlte()' method!");
//    }
//
//    @Override
//    public String toString() {
//      return documentContent.toString();
//    }
//  }
//
//  private static class SimpleIndex implements Index {
//
//    List<IndexedPage> availableDocuments;
//
//    public SimpleIndex() {
//      availableDocuments = new ArrayList<>();
//      for (int i = 0; i < TEST_DOCUMENTS.length; ++i) {
//        availableDocuments.add(new MockPage(i, TEST_DOCUMENTS[i]));
//      }
//    }
//
//    @Override
//    public Stream<IndexedPage> fetchDocuments(Query query) {
//      return availableDocuments.stream();
//    }
//  }
//
//  private static class WordFilterSearcher implements SenSeArch {
//
//    private Index index;
//
//    WordFilterSearcher(Index index) {
//      this.index = index;
//    }
//
//    private boolean filterByWord(Query query, IndexedPage document) {
//      String docContent = document.text().toString();
//      for (Term term : query.getTerms()) {
//        if (docContent.contains(term.getRaw())) {
//          return true;
//        }
//      }
//
//      return false;
//    }
//
//    @Override
//    public Stream<ResultPage> search(Query query) {
//      return index
//          .fetchDocuments(query)
//          .filter(d -> filterByWord(query, d));
//    }
//  }
//
//  private static class SimpleTerm implements Term {
//
//    private CharSequence word;
//
//    SimpleTerm(CharSequence word) {
//      this.word = word;
//    }
//
//    @Override
//    public CharSequence getRaw() {
//      return word;
//    }
//
//    @Override
//    public CharSequence getNormalized() {
//      throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public Vec getVector() {
//      return null;
//    }
//  }
//
//  private static class SimpleQuery implements Query {
//
//    private List<Term> terms;
//
//    SimpleQuery(CharSequence query) {
//      terms = new ArrayList<>();
//      for (String token : query.toString().split("\\s")) {
//        terms.add(new SimpleTerm(token));
//      }
//    }
//
//    @Override
//    public List<Term> getTerms() {
//      return terms;
//    }
//
//    @Override
//    public Vec getQueryVector() {
//      return null;
//    }
//  }
}
