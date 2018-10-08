package components.searcher;

/**
 * Created by sandulmv on 03.10.18.
 */

import components.index.Index;
import components.query.Query;
import java.util.stream.LongStream;

/**
 * The implementation of the base search.
 */
public final class SimpleSearcher {

  private Index index;
  private DocumentFilter documentFilter;

  public SimpleSearcher(Index index, DocumentFilter documentFilter) {
    this.index = index;
    this.documentFilter = documentFilter;

  }

  public long[] getSortedDocuments(Query query) {
    return fetchDocuments(query);
  }

  private long[] fetchDocuments(final Query query) {
    return
        LongStream.of(index.getDocumentIds())
            .parallel()
            .filter(
                id -> this.documentFilter.filterDocument(query, index.getDocument(id))
            )
            .toArray();
  }

  public void setDocumentFilter(DocumentFilter documentFilter) {
    this.documentFilter = documentFilter;
  }

  public void setIndex(Index index) {
    this.index = index;
  }
}
