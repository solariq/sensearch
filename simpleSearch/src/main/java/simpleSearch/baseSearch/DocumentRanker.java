package simpleSearch.baseSearch;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sandulmv on 03.10.18.
 */
public abstract class DocumentRanker {
  protected Index index;

  public DocumentRanker(Index index) {
    this.index = index;
  }

  public abstract List<DocumentId> sortDocuments(Query initialQuery, List<TermInfo> documents);

  public void setIndex(Index index) {
    this.index = index;
  }
}
