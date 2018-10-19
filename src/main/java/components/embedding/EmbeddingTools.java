package components.embedding;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import components.index.IndexedDocument;
import components.query.Query;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class EmbeddingTools {

    private static EmbeddingTools instance;

    public static synchronized EmbeddingTools getInstance() {
        if (instance == null) {
            instance = new EmbeddingTools();
        }
        return instance;
    }

    HashMap<String, Vec> stringVecHashMap;

    @SuppressWarnings("unchecked cast")
    private EmbeddingTools() {
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

    private Vec getArithmeticMean(List<Vec> vecs) {
        if (vecs.size() == 1) {
            return vecs.get(0);
        }
        Vec mean = VecTools.sum(vecs.get(0), vecs.get(1));
        for (int i = 2; i < vecs.size(); i++) {
            mean = VecTools.sum(mean, vecs.get(i));
        }
        //mean /= vecs.size();
        return mean;
    }

    <T> Vec getVec(T t) {
        if (t instanceof String) {
            return stringVecHashMap.get(t);
        } else if (t instanceof Query) {
            return getArithmeticMean(((Query) t).getTerms().stream()
                    .map(term -> getVec(term.getNormalized()))
                    .collect(Collectors.toList())
            );
        } else if (t instanceof IndexedDocument) {
            return getArithmeticMean(Arrays.stream(
                ((IndexedDocument) t)
                    .getTitle()
                    .toString()
                    .split(" ")).map(this::getVec).collect(Collectors.toList()));
        }
        throw new IllegalArgumentException("Incorrect argument for getting Vec");
    }
}
