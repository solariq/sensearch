package tools.embedding;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.function.BiFunction;

public class EmbeddingTest {

    private static final String VEC_FILENAME = "resources/vectors.txt";

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
    };

    private void neighborsTest(BiFunction<double[], double[], Double> measureFunction) {
//        for (HashMap.Entry<String, String[]> e : tests.entrySet()) {
//            for (String neighbor : e.getValue()) {
//                Assert.assertTrue(
//                    EmbeddingUtilitiesImpl
//                        .getInstance()
//                        .getNearestNeighbors(e.getKey(), measureFunction).contains(neighbor));
//            }
//        }
    }

    @Test
    public void embeddingUtilitiesEuclideanTest() {
//        neighborsTest(EmbeddingUtilitiesImpl::euclideanDistance);
    }

    @Test
    public void embeddingUtilitiesCosineTest() {
//        neighborsTest(EmbeddingUtilitiesImpl::cosineSimilarity);
    }
}
