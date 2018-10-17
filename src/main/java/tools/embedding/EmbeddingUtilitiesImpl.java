package tools.embedding;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;

public class EmbeddingUtilitiesImpl implements EmbeddingUtilities {

    private static final int NEAREST_WORD_CNT = 50;
    private static final BiFunction<double[], double[], Double> defaultMethod = EmbeddingUtilitiesImpl::euclideanDistance;

    private static EmbeddingUtilitiesImpl instance;

    public static synchronized EmbeddingUtilitiesImpl getInstance() {
        if (instance == null) {
            instance = new EmbeddingUtilitiesImpl();
        }
        return instance;
    }

    private HashMap<String, double[]> hashMap;

    @SuppressWarnings("unchecked cast")
    private EmbeddingUtilitiesImpl() {
        try(FileInputStream fis = new FileInputStream(DataFilling.SER_FILENAME);
            ObjectInputStream ois = new ObjectInputStream(fis)) {

            hashMap = (HashMap<String, double[]>) ois.readObject();

        } catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> getNearestNeighbors(String word) {
        return getNearestNeighbors(word, defaultMethod);
    }

    List<String> getNearestNeighbors(String word, BiFunction<double[], double[], Double> measureFunction) {
        double[] mainVec = hashMap.get(word);
        TreeMap<double[], String> nearestNeighbors = new TreeMap<>(Comparator.comparing(vec -> measureFunction.apply(vec, mainVec)));
        for (HashMap.Entry<String, double[]> e : hashMap.entrySet()) {
            if (e.getValue() != mainVec) {
                if (nearestNeighbors.size() < NEAREST_WORD_CNT) {
                    nearestNeighbors.put(e.getValue(), e.getKey());
                } else if (measureFunction.apply(mainVec, nearestNeighbors.lastKey()) > measureFunction.apply(mainVec, e.getValue())) {
                    nearestNeighbors.remove(nearestNeighbors.lastKey());
                    nearestNeighbors.put(e.getValue(), e.getKey());
                }
            }
        }
        return new ArrayList<>(nearestNeighbors.values());
    }

    static double euclideanDistance(double[] vec1, double[] vec2) {
        double dist = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            dist += (vec1[i] - vec2[i]) * (vec1[i] - vec2[i]);
        }
        return Math.sqrt(dist);
    }

    static double cosineSimilarity(double[] vec1, double[] vec2) {
        double dotProduct = 0.0, norm1 = 0.0, norm2 = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);
        return 1 - dotProduct / (norm1 * norm2);
    }
}
