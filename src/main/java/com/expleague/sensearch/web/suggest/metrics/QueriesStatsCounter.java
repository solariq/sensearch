package com.expleague.sensearch.web.suggest.metrics;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class QueriesStatsCounter {

  public static void printDist(int[] dist) {
    for (int i = 0; i < dist.length; i++) {
      System.out.println((i + 1) + " & " + dist[i] + "\\\\\\hline");
      //System.out.println(dist[i]);
    }
  }

  public static Map<String, List<String>> getMap() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(
        Paths.get("sugg_dataset/map").toFile(),
        new TypeReference<TreeMap<String, List<String>>>() {});
  }

  public static void listExamples() throws IOException {
    Map<String, List<String>> map = getMap();

    for (String s : map.keySet()) {
      System.out.println(s);
    }
  }
  
  public static void countDistributions() throws IOException {
    Map<String, List<String>> map = getMap();

    int[] wordCountDist = new int[10];
    int[] suffLengthDist = new int[20];

    for (String s : map.keySet()) {
      String[] splitted = s.split(" ");

      wordCountDist[splitted.length - 1]++;

      int suffLength = splitted[splitted.length - 1].length();
      suffLengthDist[suffLength - 1]++;

    }

    System.out.println("Слова");
    
    printDist(wordCountDist);

    System.out.println("Суффиксы");
    
    printDist(suffLengthDist);
  }

  public static void main(String[] args) throws IOException {
    countDistributions();
    
    //listExamples();
  }
}
