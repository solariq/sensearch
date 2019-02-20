package com.expleague.sensearch.index.plain.features;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.miner.features.QURLItem;

public class FilterFeatures extends FeatureSet.Stub<QURLItem> implements EmbeddingFeatures {
    private final static FeatureMeta TITLE = FeatureMeta
            .create("dist-title", "cos distance between Query and Title", FeatureMeta.ValueType.VEC);
    private final static FeatureMeta SECTION = FeatureMeta
            .create("dist-section", "cos distance between Query and Nearest Section Body", FeatureMeta.ValueType.VEC);
    private final static FeatureMeta LINK = FeatureMeta
            .create("dist-link", "cos distance between Query and Nearest Incoming Link", FeatureMeta.ValueType.VEC);

    private double minTitle = -1.0;
    private double minBody = -1.0;
    private double minLink = -1.0;

    @Override
    public Vec advance() {
        set(TITLE, minTitle);
        set(SECTION, minBody);
        set(LINK, minLink);
        return super.advance();
    }

    @Override
    public void withTitle(double distance) {
        minTitle = Math.max(minTitle, distance);
    }

    @Override
    public void withBody(double distance) {
        minBody = Math.max(minBody, distance);
    }

    @Override
    public void withLink(double distance) {
        minLink = Math.max(minLink, distance);
    }
}
