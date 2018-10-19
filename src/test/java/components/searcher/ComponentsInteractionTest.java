package components.searcher;

import components.index.IndexedDocument;
import components.query.term.Term;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;
import java.util.List;
import components.index.Index;
import components.query.Query;

/**
 * Created by sandulmv on 06.10.18.
 */
public class ComponentsInteractionTest {
  private static final String[] TEST_DOCUMENTS = new String[] {
      "Information Retrieval and Web Search".toLowerCase(),
      "Search Engine Ranking".toLowerCase(),
      "Web Search Course".toLowerCase()
  };

  private static class SimpleTextDocument implements IndexedDocument {
    private CharSequence documentContent;
    private long documentId;

    public SimpleTextDocument(long documentId, CharSequence documentContent) {
      this.documentContent = documentContent;
      this.documentId = documentId;
    }

    @Override
    public CharSequence getContent() {
      return documentContent;
    }

    @Override
    public long getId() {
      return documentId;
    }

    @Override
    public CharSequence getTitle() {
      throw new UnsupportedOperationException("Class does not implement 'getTitlte()' method!");
    }

    @Override
    public String toString() {
      return documentContent.toString();
    }
  }

  private static class SimpleIndex implements Index {

    List<IndexedDocument> availableDocuments;

    public SimpleIndex() {
      availableDocuments = new ArrayList<>();
      for (int i = 0; i < TEST_DOCUMENTS.length; ++i) {
        availableDocuments.add(new SimpleTextDocument(i, TEST_DOCUMENTS[i]));
      }
    }

    @Override
    public Stream<IndexedDocument> fetchDocuments(Query query) {
      return availableDocuments.stream();
    }
  }

  private static class WordFilterSearcher extends Searcher {

    WordFilterSearcher(Index index) {
      super(index);
    }

    private boolean filterByWord(Query query, IndexedDocument document) {
      String docContent = document.getContent().toString();
      for (Term term : query.getTerms()) {
        if (docContent.contains(term.getRaw())) {
          return true;
        }
      }

      return false;
    }

    @Override
    public List<IndexedDocument> getSortedDocuments(Query query) {
      return index
          .fetchDocuments(query)
          .filter(d -> filterByWord(query, d))
          .collect(Collectors.toList());
    }
  }

  private static class SimpleTerm implements Term {
    private CharSequence word;
    SimpleTerm(CharSequence word) {
      this.word = word;
    }

    @Override
    public CharSequence getRaw() {
      return word;
    }

    @Override
    public CharSequence getNormalized() {
      throw new UnsupportedOperationException();
    }
  }

  private static class SimpleQuery implements Query {
    private List<Term> terms;

    SimpleQuery(CharSequence query) {
      terms = new ArrayList<>();
      for (String token : query.toString().split("\\s")) {
        terms.add(new SimpleTerm(token));
      }
    }

    @Override
    public List<Term> getTerms() {
      return terms;
    }
  }

  @Test
  public void test() {
    Searcher wordFilterSearcher = new WordFilterSearcher(new SimpleIndex());
    Query query = new SimpleQuery("web search");
    List<IndexedDocument> foundDocuments = wordFilterSearcher.getSortedDocuments(query);
    System.out.print(foundDocuments);
    Assert.assertEquals(foundDocuments.size(), 3);
  }
}
