package simpleSearch.baseSearch;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by sandulmv on 03.10.18.
 */

// TODO: should be singleton?

/**
 * The implementation of the base search.
 */
public final class SimpleSearcher {

  private InvertedIndex invertedIndex;
  private DocumentRanker documentRanker;

  public SimpleSearcher(InvertedIndex invertedIndex, DocumentRanker documentRanker) {
    this.invertedIndex = invertedIndex;
    this.documentRanker = documentRanker;
  }

  public List<DocumentId> getRankedDocuments(Query query) {
    return documentRanker.sortDocuments(
        query, invertedIndex.getRelatedDocuments(query.getTermsList())
    );
  }

  void setInvertedIndex(InvertedIndex invertedIndex) {
    this.invertedIndex = invertedIndex;
  }


  void setDocumentRanker(DocumentRanker documentRanker) {
    this.documentRanker = documentRanker;
  }
}
