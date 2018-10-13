package components.searcher;

import components.index.Document;
import components.index.Index;
import components.query.Query;
import java.util.List;

public abstract class Searcher {

  Index index;

  Searcher(Index index) {
    this.index = index;
  }

  public abstract List<Document> getSortedDocuments(Query query);

  public void setIndex(Index index) {
    this.index = index;
  }
}
