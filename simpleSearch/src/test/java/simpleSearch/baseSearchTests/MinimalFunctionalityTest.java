package simpleSearch.baseSearchTests;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import simpleSearch.baseSearch.Document;
import simpleSearch.baseSearch.DocumentId;
import simpleSearch.baseSearch.DocumentRanker;
import simpleSearch.baseSearch.Index;
import simpleSearch.baseSearch.InvertedIndex;
import simpleSearch.baseSearch.Query;
import simpleSearch.baseSearch.SimpleSearcher;
import simpleSearch.baseSearch.Term;
import simpleSearch.baseSearch.TermInfo;
import simpleSearch.baseSearch.TfIdfRanker;

public class MinimalFunctionalityTest {

  private static final String[] DOCUMENTS = new String[]{
      "Information Retrieval and Web Search",
      "Search Engine Ranking",
      "Web Search Course"
  };
  private SimpleSearcher searcher;

  @Before
  public void init() {
    DocumentRanker ranker = new TfIdfRanker(new Index() {
      @Override
      public Document getDocument(DocumentId documentId) {
        return null;
      }

      @Override
      public long getIndexSize() {
        return 0;
      }
    });

    InvertedIndex invertedIndex = new SimpleInvIndex();

    searcher = new SimpleSearcher(invertedIndex, ranker);
  }

  @Test
  public void functionalityTest() {
    List<DocumentId> queryResult;
    queryResult = searcher.getRankedDocuments(new Query("web"));
    Assert.assertEquals(queryResult.size(), 2);
    Assert.assertEquals(queryResult.get(0).getDocumentId(), 0);
    Assert.assertEquals(queryResult.get(1).getDocumentId(), 2);
  }

  private static class SimpleInvIndex implements InvertedIndex {

    private Map<Term, TermInfo> invertedIndexMap = new HashMap<>();

    public SimpleInvIndex() {
      int docsCount = DOCUMENTS.length;
      Map<String, double[]> frequencies = new HashMap<>();
      for (int i = 0; i < DOCUMENTS.length; ++i) {
        String[] tokens = DOCUMENTS[i].toLowerCase().split("\\s");
        int tokensCount = tokens.length;
        for (String token : tokens) {
          if (!frequencies.containsKey(token)) {
            frequencies.put(token, new double[docsCount]);
          }
          frequencies.get(token)[i] += 1. / tokensCount;
        }
      }

      for (Map.Entry<String, double[]> entry : frequencies.entrySet()) {
        Term term = new Term(entry.getKey());
        double inverseDocFreq = Math.log(docsCount / entry.getValue().length);
        Map<DocumentId, Double> docFreq = new HashMap<>();
        double[] termFrequencies = entry.getValue();
        for (int i = 0; i < docsCount; ++i) {
          if (termFrequencies[i] != 0) {
            docFreq.put(new DocumentId(i), termFrequencies[i]);
          }
        }

        invertedIndexMap.put(term, new TermInfo(term, inverseDocFreq, docFreq));
      }
    }

    @Override
    public List<TermInfo> getRelatedDocuments(List<Term> terms) {
      List<TermInfo> relatedInfos = new LinkedList<>();
      for (Term term : terms) {
        relatedInfos.add(invertedIndexMap.get(term));
      }

      return relatedInfos;
    }

    @Override
    public long getTermsCount() {
      return invertedIndexMap.size();
    }

    @Override
    public long getDocumentsCount() {
      return DOCUMENTS.length;
    }

    @Override
    public Date lastTimeUpdated() {
      return new Date(System.currentTimeMillis());
    }
  }
}
