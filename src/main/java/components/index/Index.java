package components.index;


import components.query.Query;

import java.util.stream.Stream;

/**
 * Created by sandulmv on 06.10.18.
 */
public interface Index {
    Stream<IndexedDocument> fetchDocuments(Query query);
}
