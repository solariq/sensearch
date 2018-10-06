package simpleSearch.baseSearch;

import gnu.trove.list.TLongList;
import simpleSearch.queryTmp.Query;

/**
 * Created by sandulmv on 06.10.18.
 */
public class TfIdfRanker extends DocumentRanker {
  public TfIdfRanker(Index index) {
    super(index);
  }

  @Override
  public TLongList sortDocuments(Query query, TLongList documentIds) {
    return documentIds;
  }
}
