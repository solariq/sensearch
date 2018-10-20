package components.searcher;

import components.index.IndexedDocument;
import components.index.Index;
import components.query.Query;
import java.util.List;

public interface Searcher {

  List<IndexedDocument> getRankedDocuments(Query query);

}
