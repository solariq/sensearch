package com.expleague.sensearch.core;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import java.util.List;

public interface Embedding {

  /**
   * Returns vector for the word
   *
   * @param word, for which you need vector
   * @return vector
   */
  Vec getVec(String word);

  /**
   * Returns vector for the document by document id
   *
   * @param documentId, for which you need vector
   * @return vector
   */
  Vec getVec(Long documentId);

  /**
   * Returns vector for the query
   *
   * @param query, for which you need vector
   * @return vector
   */
  Vec getVec(Query query);

  Vec getVec(List<Term> terms);

  /**
   * Returns list of vectors for query's terms
   *
   * @param query, for which you need vectors
   * @return list of vectors
   */
  List<Vec> getVecsForTerms(Query query);

  /**
   * Returns list of the nearest words
   *
   * @param mainVec, for which you need nearest words
   * @param numberOfNeighbors, how many neighbors you need
   * @return list of words
   */
  List<String> getNearestWords(Vec mainVec, int numberOfNeighbors);

  /**
   * Returns list of the nearest documents
   *
   * @param mainVec, for which you need nearest words
   * @param numberOfNeighbors, how many neighbors you need
   * @return list of ids of documents
   */
  List<Long> getNearestDocuments(Vec mainVec, int numberOfNeighbors);
}
