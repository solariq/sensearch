package components.embedding.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import components.embedding.NearestFinder;

import java.util.*;
import java.util.function.BiFunction;

public class NearestFinderImpl implements NearestFinder {

  private static final BiFunction<Vec, Vec, Double> defaultMeasure = VecTools::distanceAV;
  private static final double epsilon = 10e-9;

  @Override
  public List<String> getNearestWords(Vec mainVec, int numberOfNeighbors) {
    return getNearest(mainVec, numberOfNeighbors, EmbeddingImpl.getInstance().getWordVecMap(),
        defaultMeasure);
  }

  @Override
  public List<Long> getNearestDocuments(Vec mainVec, int numberOfNeighbors) {
    return getNearest(mainVec, numberOfNeighbors, EmbeddingImpl.getInstance().getDocIdVecMap(),
        defaultMeasure);
  }

  private <T> List<T> getNearest(Vec mainVec, int numberOfNeighbors, Map<T, Vec> map,
      BiFunction<Vec, Vec, Double> measureFunction) {
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
    for (Map.Entry<T, Vec> e : map.entrySet()) {
      if (e.getValue() != mainVec) {
        if (nearest.size() < numberOfNeighbors) {
          nearest.put(e.getValue(), e.getKey());
        } else if (measureFunction.apply(mainVec, nearest.lastKey()) > measureFunction
            .apply(mainVec, e.getValue())) {
          nearest.remove(nearest.lastKey());
          nearest.put(e.getValue(), e.getKey());
        }
      }
    }
    return new ArrayList<>(nearest.values());
  }
}
