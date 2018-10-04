package simpleSearch.baseSearch;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sandulmv on 03.10.18.
 */
public interface InvertedIndex {
  Map<Term, Set<DocumentInfo>> getRelatedDocuments(List<Term> terms);
  // Maybe get some general information about index?
  long getTermCount();
  long getDocumentsCount();
  Date lastTimeUpdated();
}
