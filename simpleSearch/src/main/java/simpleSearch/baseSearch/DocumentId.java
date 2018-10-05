package simpleSearch.baseSearch;

/**
 * Created by sandulmv on 03.10.18.
 */
public class DocumentId {
  private long documentId;

  public DocumentId(long documentId) {
    this.documentId = documentId;
  }

  public long getDocumentId() {
    return documentId;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof DocumentId)) {
      return false;
    }

    DocumentId otherId = (DocumentId) other;
    return otherId.documentId == this.documentId;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(documentId);
  }
}
