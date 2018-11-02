package com.expleague.sensearch.miner;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.query.Query;

/**
 * Created by sandulmv on 01.11.18.
 */
public interface FeaturesMiner {
  Features extractFeatures(Query query, Page page);
}
