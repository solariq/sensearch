package simpleSearch.baseSearch;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sandulmv on 03.10.18.
 * Assumed interface of InvertedIndex
 */
public interface InvertedIndex {
  List<TermInfo> getRelatedDocuments(List<Term> terms);
  // Maybe get some general information about index?
  long getTermsCount();
  long getDocumentsCount();
  Date lastTimeUpdated();
}
