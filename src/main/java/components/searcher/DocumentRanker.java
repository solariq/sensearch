package components.searcher;

import gnu.trove.list.TLongList;
import components.queryTmp.Query;

/**
 * Created by sandulmv on 03.10.18.
 */
public abstract class DocumentRanker {
  protected Index index;

  public DocumentRanker(Index index) {
    this.index = index;
  }

  public abstract TLongList sortDocuments(Query initialQuery, TLongList documents);

  public void setIndex(Index index) {
    this.index = index;
  }
}
