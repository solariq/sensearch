package tools.embedding;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import components.index.Index;
import components.index.IndexedDocument;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;

public class EmbeddingUtilitiesImpl implements EmbeddingUtilities {

    private static final BiFunction<Vec, Vec, Double> defaultMethod = VecTools::distanceAV;
    private static final double epsilon = 10e-9;

    private static EmbeddingUtilitiesImpl instance;

    public static synchronized EmbeddingUtilitiesImpl getInstance() {
        if (instance == null) {
            instance = new EmbeddingUtilitiesImpl();
        }
        return instance;
    }

    private HashMap<String, Vec> stringVecHashMap;

    @SuppressWarnings("unchecked cast")
    private EmbeddingUtilitiesImpl() {
        try(FileInputStream fis = new FileInputStream(DataFilling.SER_FILENAME);
            ObjectInputStream ois = new ObjectInputStream(fis)) {

            HashMap<String, double[]> help = (HashMap<String, double[]>) ois.readObject();

            stringVecHashMap = new HashMap<>();
            for (HashMap.Entry<String, double[]> e : help.entrySet()) {
                stringVecHashMap.put(e.getKey(), new ArrayVec(e.getValue()));
            }

        } catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> getNearestNeighbors(String word, int numberOfNeighbors) {
        return getNearestNeighbors(word, numberOfNeighbors, defaultMethod);
    }

    @Override
    public List<IndexedDocument> getNearestDocuments(IndexedDocument document, int numberOfNeighbors) {
        return getNearestNeighbors(document, );
    }

    List<String> getNearestNeighbors(String word, int numberOfNeighbors, BiFunction<Vec, Vec, Double> measureFunction) {
        return getNearestNeighbors(word, stringVecHashMap, numberOfNeighbors, measureFunction);
    }

    private <T> List<T> getNearestNeighbors(T main, HashMap<T, Vec> hashMap, int numberOfNeighbors, BiFunction<Vec, Vec, Double> measureFunction) {
        Vec mainVec = hashMap.get(main);
        TreeMap<Vec, T> nearestNeighbors = new TreeMap<>(
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
                if (nearestNeighbors.size() < numberOfNeighbors) {
                    nearestNeighbors.put(e.getValue(), e.getKey());
                } else if (measureFunction.apply(mainVec, nearestNeighbors.lastKey()) > measureFunction.apply(mainVec, e.getValue())) {
                    nearestNeighbors.remove(nearestNeighbors.lastKey());
                    nearestNeighbors.put(e.getValue(), e.getKey());
                }
            }
        }
        return new ArrayList<>(nearestNeighbors.values());
    }
}
