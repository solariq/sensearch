package com.expleague.sensearch.index;


import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Embedding;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by sandulmv on 06.10.18.
 */
public interface Index extends IndexStatistics {

  Stream<Page> fetchDocuments(Query query);

}
