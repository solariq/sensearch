package tools.embedding;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class EmbeddingTest {

    private static final String VEC_FILENAME = "resources/vectors.txt";

    @Test
    public void dataFillingTest() {
        DataFilling dataFilling = new DataFilling(VEC_FILENAME);
        dataFilling.fill();
        dataFilling.save();
        Assert.assertTrue(new File(DataFilling.SER_FILENAME).exists());
    }

    private boolean contains(String word, String sinonim) {
        return EmbeddingUtilitiesImpl.getInstance().getNearestSemanticWords(word).contains(sinonim);
    }

    @Test
    public void embeddingUtilitiesTest() {
        Assert.assertTrue(contains("женщина", "девушка"));
        Assert.assertTrue(contains("женщина", "молодая"));
        Assert.assertTrue(contains("женщина", "красивая"));
        Assert.assertTrue(contains("женщина", "беременная"));
        Assert.assertTrue(contains("женщина", "мать"));
        Assert.assertTrue(contains("вода", "пресная"));
        Assert.assertTrue(contains("вода", "влага"));
        Assert.assertTrue(contains("вода", "рыба"));
        Assert.assertTrue(contains("вода", "жидкость"));
        Assert.assertTrue(contains("вода", "питьевая"));
        Assert.assertTrue(contains("телефон", "мобильный"));
        Assert.assertTrue(contains("телефон", "звонок"));
        Assert.assertTrue(contains("телефон", "сотовый"));
        Assert.assertTrue(contains("телефон", "компьютер"));
        Assert.assertTrue(contains("телефон", "интернет"));
    }
}
