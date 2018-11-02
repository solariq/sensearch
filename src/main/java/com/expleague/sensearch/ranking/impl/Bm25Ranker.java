package com.expleague.sensearch.ranking.impl;

import com.expleague.sensearch.miner.Features;
import com.expleague.sensearch.ranking.PointWiseRanker;

/**
 * Created by sandulmv on 02.11.18.
 */
public class Bm25Ranker implements PointWiseRanker {
  @Override
  public double rank(Features textFeatures) {
    return textFeatures.bm25();
  }
}
