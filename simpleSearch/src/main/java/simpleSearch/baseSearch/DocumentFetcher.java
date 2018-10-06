package simpleSearch.baseSearch;

import gnu.trove.list.TLongList;
import simpleSearch.queryTmp.Query;

/**
 * Created by sandulmv on 06.10.18.
 */
public abstract class DocumentFetcher {
  protected Index index;

  public DocumentFetcher(Index index) {
    this.index = index;
  }

  public abstract TLongList fetchDocuments(Query query);

  public void setIndex(Index index) {
    this.index = index;
  }
}
