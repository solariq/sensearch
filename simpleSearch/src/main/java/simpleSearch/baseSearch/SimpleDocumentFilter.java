package simpleSearch.baseSearch;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by sandulmv on 03.10.18.
 */
public class SimpleDocumentFilter implements DocumentFilter {

  @Override
  public Set<DocumentInfo> filterDocuments(Query initialQuery,
      Map<Term, Set<DocumentInfo>> documentSplits) {
    Set<DocumentInfo> intersection = new HashSet<>(
        documentSplits.values().iterator().next()
    );

    for (Set<DocumentInfo> documents : documentSplits.values()) {
      intersection.retainAll(documents);
    }

    return intersection;
  }
}
