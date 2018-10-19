package components.embedding;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.function.BiFunction;

public class EmbeddingTest {

    private static final String VEC_FILENAME = "resources/vectors.txt";
    private static final int numberOfNeighbors = 50;

    /*@Test
    public void dataFillingTest() {
        DataFilling dataFilling = new DataFilling(VEC_FILENAME);
        dataFilling.fill();
        dataFilling.save();
        Assert.assertTrue(new File(DataFilling.SER_FILENAME).exists());
    }*/

    private static final HashMap<String, String[]> tests;
    static {
        HashMap<String, String[]> hashMap = new HashMap<>();
        hashMap.put("женщина", new String[]{"девушка", "девочка", "молодая", "красивая", "беременная"});
        hashMap.put("вода", new String[]{"пресная", "влага", "рыба", "солёная", "питьевая"});
        hashMap.put("телефон", new String[]{"мобильный", "звонок", "сотовый", "компьютер", "клиент"});
        tests = hashMap;
    }

    private void neighborsTest(BiFunction<Vec, Vec, Double> measureFunction) {
        NearestFinderImpl<String> nearestFinder = new NearestFinderImpl<>();
        for (HashMap.Entry<String, String[]> e : tests.entrySet()) {
            for (String neighbor : e.getValue()) {
                Assert.assertTrue(nearestFinder.getNearest(e.getKey(), numberOfNeighbors, measureFunction).contains(neighbor));
            }
        }
    }

    @Test
    public void embeddingUtilitiesEuclideanTest() {
        neighborsTest(VecTools::distanceAV);
    }

    @Test
    public void embeddingUtilitiesCosineTest() {
        neighborsTest((vec1, vec2) -> 1 - VecTools.cosine(vec1, vec2));
    }
}
