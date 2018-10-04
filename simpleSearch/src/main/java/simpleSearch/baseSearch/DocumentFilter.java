package simpleSearch.baseSearch;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sandulmv on 03.10.18.
 */
public interface DocumentFilter {
  Set<DocumentInfo> filterDocuments(Query initialQuery, Map<Term, Set<DocumentInfo>> documents);
}
