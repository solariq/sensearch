package com.expleague.sensearch.miner;

/**
 * Created by sandulmv on 01.11.18.
 */
public interface Features {
  double bm25();
  double fuzzy();
  double lm();
  double dlh();
  double dllh();
}
