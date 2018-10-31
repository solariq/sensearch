package com.expleague.sensearch.core.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.sensearch.core.NearestFinder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

public class NearestFinderImpl implements NearestFinder {

  private static final BiFunction<Vec, Vec, Double> DEFAULT_MEASURE = VecTools::distanceAV;
  private static final double EPSILON = 10e-9;

  @Override
  public List<String> getNearestWords(Vec mainVec, int numberOfNeighbors) {
    return getNearest(mainVec, numberOfNeighbors, EmbeddingImpl.getInstance().getWordVecMap(),
        DEFAULT_MEASURE);
  }

  @Override
  public List<Long> getNearestDocuments(Vec mainVec, int numberOfNeighbors) {
    return getNearest(mainVec, numberOfNeighbors, EmbeddingImpl.getInstance().getDocIdVecMap(),
        DEFAULT_MEASURE);
  }

  private <T> List<T> getNearest(Vec mainVec, int numberOfNeighbors, Map<T, Vec> map,
      BiFunction<Vec, Vec, Double> measureFunction) {
    TreeMap<Vec, T> nearest = new TreeMap<>(
        (vec1, vec2) -> {
          double val1 = measureFunction.apply(mainVec, vec1);
          double val2 = measureFunction.apply(mainVec, vec2);
          if (Math.abs(val1 - val2) < EPSILON) {
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
