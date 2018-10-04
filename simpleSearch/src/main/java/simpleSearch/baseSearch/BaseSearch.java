package simpleSearch.baseSearch;

import java.util.List;

/**
 * Created by sandulmv on 03.10.18.
 */

// should be singleton?
public final class BaseSearch {

  private InvertedIndex invertedIndex;
  private DocumentFilter documentFilter;
  private DocumentRanker documentRanker;

  BaseSearch(InvertedIndex invertedIndex, DocumentFilter documentFilter,
      DocumentRanker documentRanker) {
    this.invertedIndex = invertedIndex;
    this.documentFilter = documentFilter;
    this.documentRanker = documentRanker;
  }

  public List<DocumentId> getRankedDocuments(Query query) {
    return null;
  }

  void setInvertedIndex(InvertedIndex invertedIndex) {
    this.invertedIndex = invertedIndex;
  }

  void setDocumentFilter(DocumentFilter documentFilter) {
    this.documentFilter = documentFilter;
  }

  void setDocumentRanker(DocumentRanker documentRanker) {
    this.documentRanker = documentRanker;
  }
}
