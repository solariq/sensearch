package simpleSearch.baseSearch;

/**
 * Created by sandulmv on 04.10.18.
 */
public interface Index {
  Document getDocument(DocumentId documentId);
  long getIndexSize();
}
