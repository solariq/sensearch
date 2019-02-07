package com.expleague.sensearch.experiments;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DCGSummator {

  public static void main(String[] args) {
    try {
      BufferedReader br = Files.newBufferedReader(Paths.get("./src/main/java/com/expleague/sensearch/experiments/dcg_logs"));
      String line;
      double sum = 0;
      while ((line = br.readLine()) != null) {
        line = br.readLine();
        System.out.println(line);
        sum += Double.parseDouble(line.substring(line.lastIndexOf(':') + 2, line.lastIndexOf('%')));
        line = br.readLine();
      }
      System.out.println(sum / 120);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}
