package components.searcher;

import components.query.BaseQuery;
import components.query.term.Term;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import org.junit.Assert;
import org.junit.Test;
import java.util.List;
import components.index.Index;
import components.crawler.document.CrawlerDocument;
import components.query.Query;

/**
 * Created by sandulmv on 06.10.18.
 */
public class MinimalFunctionalityTest {
  private static final String[] TEST_DOCUMENTS = new String[] {
      "Information Retrieval and Web Search".toLowerCase(),
      "Search Engine Ranking".toLowerCase(),
      "Web Search Course".toLowerCase()
  };

  private static class SimpleTextDocument implements CrawlerDocument {
    private String documentContent;
    public SimpleTextDocument(String documentContent) {
      this.documentContent = documentContent;
    }

    @Override
    public boolean checkWord(String word) {
      return documentContent.contains(word);
    }

    @Override
    public List<Boolean> checkWords(List<String> words) {
      List<Boolean> includes = new ArrayList<>(words.size());
      for (String word : words) {
        includes.add(checkWord(word));
      }

      return includes;
    }

    @Override
    public List<CharSequence> returnSentences(String word) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CharSequence returnContent() {
      return documentContent;
    }

    @Override
    public String getTitle() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Long getID() {
      return null;
    }

  }

  private static class SimpleIndex implements Index {

    private CrawlerDocument[] indexedDocuments;

    public SimpleIndex() {
      indexedDocuments = new CrawlerDocument[TEST_DOCUMENTS.length];
      for (int i = 0; i < TEST_DOCUMENTS.length; ++i) {
        indexedDocuments[i] = new SimpleTextDocument(TEST_DOCUMENTS[i]);
      }
    }

    @Override
    public CrawlerDocument getDocument(long documentId) {
      if (documentId < 0 && documentId >= indexedDocuments.length) {
        throw new NoSuchElementException();
      }
      return indexedDocuments[(int) documentId];
    }

    @Override
    public long[] getDocumentIds() {
      long[] ids = new long[indexedDocuments.length];
      for (int i = 0; i < ids.length; ++i) {
        ids[i] = i;
      }
      return ids;
    }

    @Override
    public int size() {
      return indexedDocuments.length;
    }
  }

  public class SimpleDocumentFilter implements DocumentFilter {
    @Override
    public boolean filterDocument(Query query, CrawlerDocument document) {
      for (Term queryTerm : query.getTerms()) {
        if (document.checkWord(queryTerm.getRaw().toString())) {
          return true;
        }
      }

      return false;
    }
  }

  @Test
  public void test() {
    Index index = new SimpleIndex();
    DocumentFilter filter = new SimpleDocumentFilter();

    SimpleSearcher searcher = new SimpleSearcher(index, filter);
    long[] foundDocuments = searcher.getSortedDocuments(new BaseQuery("web"));
    Assert.assertEquals(foundDocuments.length, 2);
  }
}
