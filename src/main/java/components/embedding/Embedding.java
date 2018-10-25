package components.embedding;

import com.expleague.commons.math.vectors.Vec;
import components.query.Query;

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

    /**
     * Returns list of vectors for query's terms
     *
     * @param query, for which you need vectors
     * @return list of vectors
     */
    List<Vec> getVecsForTerms(Query query);
}
