package components.embedding;

import java.util.List;

public interface NearestFinder<T> {

    /**
     * Returns a list of the nearest neighbors of a word
     * that is ordered from the closest neighbor to the farthest
     *
     * @param numberOfNeighbors is how many neighbors you need
     * @return list of the nearest neighbors
     */
     List<T> getNearest(T t, int numberOfNeighbors);
}
