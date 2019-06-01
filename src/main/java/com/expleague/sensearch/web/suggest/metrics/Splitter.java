package com.expleague.sensearch.web.suggest.metrics;

import java.util.Random;

public class Splitter {
  private Random rnd = new Random(0);
  
  private int block_counter = 0;
  private int curr_rand;
  
  private void nextCheck() {
    if (block_counter == 0) {
      block_counter = 10;
      curr_rand = rnd.nextInt(6000);
    }
    block_counter--;
  }
  
  public boolean isTrain() {
    nextCheck();
    return curr_rand > 2000;
  }
  
  public boolean isTest() {
    nextCheck();
    return curr_rand <= 2000;
  }
}
