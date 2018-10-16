package tools.embedding;

import java.io.*;
import java.util.*;

public class EmbeddingUtilitiesImpl implements EmbeddingUtilities {

    private static EmbeddingUtilitiesImpl instance;

    private static final int NEAREST_WORD_CNT = 50;

    private HashMap<String, double[]> hashMap;

    public static synchronized EmbeddingUtilitiesImpl getInstance() {
        if (instance == null) {
            instance = new EmbeddingUtilitiesImpl();
        }
        return instance;
    }

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
    public List<String> getNearestSemanticWords(String word) {
        double[] mainVec = hashMap.get(word);
        TreeMap<double[], String> treeMap = new TreeMap<>(Comparator.comparing(vec -> cosine(mainVec, vec)));
        for (HashMap.Entry<String, double[]> e : hashMap.entrySet()) {
            if (e.getValue() != mainVec) {
                if (treeMap.size() < NEAREST_WORD_CNT) {
                    treeMap.put(e.getValue(), e.getKey());
                } else if (cosine(mainVec, treeMap.lastKey()) > cosine(mainVec, e.getValue())) {
                    treeMap.remove(treeMap.lastKey());
                    treeMap.put(e.getValue(), e.getKey());
                }
            }
        }
        return new ArrayList<>(treeMap.values());
    }

    private double cosine(double[] v1, double[] v2) {
        double dotProduct = 0.0, norm1 = 0.0, norm2 = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        norm1 = Math.sqrt(norm1);
        norm2 = Math.sqrt(norm2);
        return 1.0 - dotProduct / (norm1 * norm2);
    }
}
