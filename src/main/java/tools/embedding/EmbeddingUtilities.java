package tools.embedding;

import java.util.List;

public interface EmbeddingUtilities {

    /**
     * Returns a list of the nearest neighbors of a word
     * that is ordered from the closest neighbor to the farthest
     *
     * @param word, for which you need to find neighbors
     * @return list of the nearest neighbors
     */
    List<String> getNearestNeighbors(String word);
}
