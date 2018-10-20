package components.searcher;

import components.index.IndexedDocument;
import components.index.Index;
import components.query.Query;
import java.util.List;

public abstract class Searcher {

  Index index;

  Searcher(Index index) {
    this.index = index;
  }

  public abstract List<IndexedDocument> getRankedDocuments(Query query);

  public void setIndex(Index index) {
    this.index = index;
  }
}
