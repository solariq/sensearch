package simpleSearch;

import com.expleague.commons.util.Pair;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import java.util.ArrayList;
import java.util.Iterator;
import org.junit.Assert;
import org.junit.Test;
import java.util.List;
import simpleSearch.baseSearch.DocumentFetcher;
import simpleSearch.baseSearch.DocumentRanker;
import simpleSearch.baseSearch.Index;
import simpleSearch.baseSearch.SimpleSearcher;
import simpleSearch.crawler.document.CrawlerDocument;
import simpleSearch.queryTmp.Query;
import simpleSearch.queryTmp.Term;

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
    private String documentConent;
    public SimpleTextDocument(String documentConent) {
      this.documentConent = documentConent;
    }

    @Override
    public boolean checkWord(String word) {
      return documentConent.contains(word);
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
      return documentConent;
    }

    @Override
    public String getTitle() {
      throw new UnsupportedOperationException();
    }

  }

  private static class SimpleIndex extends Index {

    private CrawlerDocument[] indexedDocuments;

    public SimpleIndex() {
      super("");
      indexedDocuments = new CrawlerDocument[TEST_DOCUMENTS.length];
      for (int i = 0; i < TEST_DOCUMENTS.length; ++i) {
        indexedDocuments[i] = new SimpleTextDocument(TEST_DOCUMENTS[i]);
      }
    }

    @Override
    public CrawlerDocument getDocument(long documentId) {
      if (documentId >= 0 && documentId < indexedDocuments.length) {
        return indexedDocuments[(int) documentId];
      }

      throw new IndexOutOfBoundsException();
    }

    @Override
    public Iterator<Pair<Long, CrawlerDocument>> iterator() {
      return new Iterator<Pair<Long, CrawlerDocument>>() {
        private int curIndex = -1;
        @Override
        public boolean hasNext() {
          return curIndex < indexedDocuments.length;
        }

        @Override
        public Pair<Long, CrawlerDocument> next() {
          ++curIndex;
          return new Pair<>((long)curIndex, indexedDocuments[curIndex]);
        }
      };
    }
  }

  private static class SimpleFetcher extends DocumentFetcher {

    public SimpleFetcher(Index index) {
      super(index);
    }

    @Override
    public TLongList fetchDocuments(Query query) {
      TLongList fetchedDocumentIds = new TLongArrayList();
      for (Pair<Long, CrawlerDocument> idAndDocument : index) {
        long documentId = idAndDocument.getFirst();
        CrawlerDocument document = idAndDocument.getSecond();
        for (Term term : query.getTermsList()) {
          if (document.checkWord(term.getRawTerm())) {
            fetchedDocumentIds.add(documentId);
          }
        }
      }

      return fetchedDocumentIds;
    }
  }

  private static class SimpleRanker extends DocumentRanker {
    public SimpleRanker(Index index) {
      super(index);
    }

    @Override
    public TLongList sortDocuments(Query query, TLongList documentIds) {
      return documentIds;
    }

  }
  @Test
  public void test() {
    Index index = new SimpleIndex();
    DocumentFetcher fetcher = new SimpleFetcher(index);
    DocumentRanker ranker = new SimpleRanker(index);
    SimpleSearcher searcher = new SimpleSearcher(fetcher, ranker);
    TLongList foundDocuments = searcher.getSortedDocuments(new Query("web"));
    Assert.assertEquals(foundDocuments.size(), 2);
  }
}
