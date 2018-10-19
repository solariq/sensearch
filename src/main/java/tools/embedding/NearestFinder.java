package tools.embedding;

import components.index.IndexedDocument;

import java.util.List;

public interface EmbeddingUtilities {

    /**
     * Returns a list of the nearest neighbors of a word
     * that is ordered from the closest neighbor to the farthest
     *
     * @param word, for which you need to find neighbors
     * @param numberOfNeighbors is how many neighbors you need
     * @return list of the nearest neighbors
     */
    List<String> getNearestNeighbors(String word, int numberOfNeighbors);

    List<IndexedDocument> getNearestDocuments(IndexedDocument indexedDocument, int numberOfNeigbors);
}
