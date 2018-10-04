package simpleSearch.baseSearch;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by sandulmv on 03.10.18.
 */
public class SimpleDocumentRanker implements DocumentRanker {
  @Override
  public List<DocumentInfo> sortDocuments(Query initialQuery, Set<DocumentInfo> documents) {
    return new LinkedList<DocumentInfo>(documents);
  }
}
