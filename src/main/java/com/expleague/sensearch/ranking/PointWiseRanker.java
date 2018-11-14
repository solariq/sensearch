package com.expleague.sensearch.ranking;

import com.expleague.sensearch.miner.Features;

/**
 * Created by sandulmv on 02.11.18.
 */
public interface PointWiseRanker {

  double rank(Features features);
}
