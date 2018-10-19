package components.embedding;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.function.BiFunction;

public class NearestFinderImpl<T> implements NearestFinder<T> {

    private static final BiFunction<Vec, Vec, Double> defaultMethod = VecTools::distanceAV;
    private static final double epsilon = 10e-9;

    private HashMap<T, Vec> hashMap;

    @SuppressWarnings("unchecked cast")
    public NearestFinderImpl() {
        hashMap = (HashMap<T, Vec>) EmbeddingTools.getInstance().stringVecHashMap;
    }

    public List<T> getNearest(T main, int numberOfNeighbors) {
        return getNearest(main, numberOfNeighbors, defaultMethod);
    }

    public List<T> getNearest(T main, int numberOfNeighbors, BiFunction<Vec, Vec, Double> measureFunction) {
        Vec mainVec = EmbeddingTools.getInstance().getVec(main);
        TreeMap<Vec, T> nearest = new TreeMap<>(
                (vec1, vec2) -> {
                    double val1 = measureFunction.apply(mainVec, vec1);
                    double val2 = measureFunction.apply(mainVec, vec2);
                    if (Math.abs(val1 - val2) < epsilon) {
                        return 0;
                    }
                    return val1 < val2 ? -1 : 1;
                }
        );
        for (HashMap.Entry<T, Vec> e : hashMap.entrySet()) {
            if (e.getValue() != mainVec) {
                if (nearest.size() < numberOfNeighbors) {
                    nearest.put(e.getValue(), e.getKey());
                } else if (measureFunction.apply(mainVec, nearest.lastKey()) > measureFunction.apply(mainVec, e.getValue())) {
                    nearest.remove(nearest.lastKey());
                    nearest.put(e.getValue(), e.getKey());
                }
            }
        }
        return new ArrayList<>(nearest.values());
    }
}
