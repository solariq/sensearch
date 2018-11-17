package com.expleague.sensearch.embedding;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.impl.EmbeddingImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
<<<<<<< HEAD

import com.expleague.sensearch.Config;
import com.expleague.sensearch.index.embedding.impl.EmbeddingImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
=======
>>>>>>> 56248c58f72e23a35bd8ee431f3c804b0402a999
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NearestFinderTest {

  private static final int numberOfNeighbors = 50;

  private static final Map<String, String[]> tests;

  static {
    Map<String, String[]> map = new HashMap<>();
    map.put("женщина", new String[]{"девушка", "девочка", "молодая", "красивая", "мать"});
    map.put("вода", new String[]{"пресная", "лёд", "воздух", "солёная", "питьевая"});
    map.put("школа",
        new String[]{"гимназия", "начальная", "общеобразовательная", "музыкальная", "спортивная"});
    tests = map;
  }

  private EmbeddingImpl embedding;

  @Before
  public void initEmbedding() throws IOException {
    byte[] jsonData = Files.readAllBytes(Paths.get("./config.json"));
    Config config = new ObjectMapper().readValue(jsonData, Config.class);
    embedding = new EmbeddingImpl(config);
  }

  private void nearestFinderTest() {
    for (Map.Entry<String, String[]> entry : tests.entrySet()) {
      List<String> nearest = embedding
          .getNearestWords(embedding.getVec(entry.getKey()), numberOfNeighbors);
      for (String neighbor : entry.getValue()) {
        Assert.assertTrue(nearest.contains(neighbor));
      }
    }
  }

  @Test
  public void euclideanTest() {
    embedding.switchMeasureToEuclidean();
    nearestFinderTest();
  }

  @Test
  public void cosineTest() {
    embedding.switchMeasureToCosine();
    nearestFinderTest();
  }
}
