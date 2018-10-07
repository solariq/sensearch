package components.searcher;

/**
 * Created by sandulmv on 03.10.18.
 */

import gnu.trove.list.TLongList;
import components.queryTmp.Query;

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

  public TLongList getSortedDocuments(Query query) {
    return documentRanker.sortDocuments(
        query, documentFetcher.fetchDocuments(query)
    );
  }

  void setDocumentRanker(DocumentRanker documentRanker) {
    this.documentRanker = documentRanker;
  }

  void setDocumentFetcher(DocumentFetcher documentFetcher) {
    this.documentFetcher = documentFetcher;
  }
}
