package simpleSearch.baseSearch;

import java.util.List;
import java.util.Set;

/**
 * Created by sandulmv on 03.10.18.
 */
public interface DocumentRanker {
  List<DocumentInfo> sortDocuments(Query initialQuery, Set<DocumentInfo> documents);
}
