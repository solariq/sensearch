package simpleSearch.baseSearch;

/**
 * Created by sandulmv on 03.10.18.
 */

// TODO: should be singleton?

/**
 * The implementation of the base search.
 */
public final class SimpleSearcher {

  private DocumentRanker documentRanker;
  private DocumentFetcher documentFetcher;

  public SimpleSearcher(DocumentFetcher documentFilter, DocumentRanker documentRanker) {
    this.documentFetcher = documentFilter;
    this.documentRanker = documentRanker;
  }

  public TLongList getRankedDocuments(Query query) {
    return documentRanker.sortDocuments(
        query, documentFetcher.fetchDocumentsFromIndex(query)
    );
  }

  void setDocumentRanker(DocumentRanker documentRanker) {
    this.documentRanker = documentRanker;
  }

  void setDocumentFetcher(DocumentFetcher documentFetcher) {
    this.documentFetcher = documentFetcher;
  }
}
