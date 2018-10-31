package com.expleague.sensearch;

import com.expleague.sensearch.index.IndexedDocument;
import com.expleague.sensearch.query.Query;
import java.util.List;

public interface SenSeArch {

  List<IndexedDocument> getRankedDocuments(Query query);

}
