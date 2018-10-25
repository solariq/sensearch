package components.embedding;

import com.expleague.commons.math.vectors.Vec;

import java.util.List;

public interface NearestFinder {

    /**
     * Returns list of the nearest words
     *
     * @param mainVec,           for which you need nearest words
     * @param numberOfNeighbors, how many neighbors you need
     * @return list of words
     */
    List<String> getNearestWords(Vec mainVec, int numberOfNeighbors);

    /**
     * Returns list of the nearest documents
     *
     * @param mainVec,           for which you need nearest words
     * @param numberOfNeighbors, how many neighbors you need
     * @return list of ids of documents
     */
    List<Long> getNearestDocuments(Vec mainVec, int numberOfNeighbors);
}
