package components.embedding;

import components.query.Query;
import java.util.stream.Stream;

public interface Filter {

  /**
   * Returns stream of the nearest (for query) documents
   *
   * @param query, for which you need nearest documents
   * @return stream of ids of documents
   */
  Stream<Long> filtrate(Query query);
}
