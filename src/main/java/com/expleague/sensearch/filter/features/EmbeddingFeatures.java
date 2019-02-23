package com.expleague.sensearch.filter.features;

import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.sensearch.miner.features.QURLItem;

public interface EmbeddingFeatures extends FeatureSet<QURLItem> {

    void withTitle(double distance);
    void withBody(double distance);
    void withLink(double distance);
}
