package com.expleague.sensearch.index.embedding.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.LongStream;

class Storage {

    private static final double EPSILON = 10e-9;

    private final int cacheCapacity = 10000;

    private Map<Long, double[]> map;

    private final Map<Long, Vec> lruCache = new LinkedHashMap<Long, Vec>(cacheCapacity, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Vec> eldest) {
            return size() > cacheCapacity;
        }
    };

    Storage() {
        //DB db = DBMaker.tempFileDB().make();
        //map = db.hashMap("vectors").keySerializer(Serializer.LONG).valueSerializer(Serializer.DOUBLE_ARRAY).createOrOpen();
    }

    void put(long id, double[] coords) {
        map.put(id, coords);
    }

    Vec get(long id) {
        if (lruCache.containsKey(id)) {
            return lruCache.get(id);
        }
        Vec vec = new ArrayVec(map.get(id));
        lruCache.put(id, vec);
        return vec;
    }

    LongStream getNearest(double[] mainCoords, int numberOfNeighbors) {
        Comparator<double[]> comparator = getComparator(mainCoords);
        TreeMap<double[], Long> nearest = new TreeMap<>(comparator);
        for (Map.Entry<Long, double[]> entry : map.entrySet()) {
            long id = entry.getKey();
            double[] coords = entry.getValue();
            if (nearest.size() < numberOfNeighbors) {
                nearest.put(coords, id);
            } else if (comparator.compare(nearest.lastKey(), coords) > 0) {
                nearest.pollLastEntry();
                nearest.put(coords, id);
            }
        }
        return nearest.values().stream().mapToLong(Long::longValue);
    }

    private double euclidean(double[] coords1, double[] coords2) {
        double sum = 0.;
        for (int i = 0; i < coords1.length; i++) {
            double dif = coords1[i] - coords2[i];
            sum += dif * dif;
        }
        return Math.sqrt(sum);
    }

    private Comparator<double[]> getComparator(double[] mainCoords) { ;
        return (coords1, coords2) -> {
            double val1 = euclidean(mainCoords, coords1);
            double val2 = euclidean(mainCoords, coords2);
            if (Math.abs(val1 - val2) < EPSILON) {
                return 0;
            }
            return val1 < val2 ? -1 : 1;
        };
    }
}