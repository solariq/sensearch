package com.expleague.sensearch.core;

import com.expleague.sensearch.query.Query;

import java.util.stream.LongStream;

public interface Filter {

  /**
   * Returns stream of the nearest (for query) documents
   *
   * @param query, for which you need nearest documents
   * @return stream of ids of documents
   */
  LongStream filtrate(Query query);
}
