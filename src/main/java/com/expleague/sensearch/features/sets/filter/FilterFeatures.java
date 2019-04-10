package com.expleague.sensearch.features.sets.filter;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.features.QURLItem;

public class FilterFeatures extends FeatureSet.Stub<QURLItem> implements EmbeddingFeatures {
    public final static FeatureMeta TITLE = FeatureMeta
            .create("dist-title", "cos distance between Query and Title", FeatureMeta.ValueType.VEC);
    public final static FeatureMeta SECTION = FeatureMeta
            .create("dist-section", "cos distance between Query and Nearest Section Body", FeatureMeta.ValueType.VEC);
    public final static FeatureMeta LINK = FeatureMeta
            .create("dist-link", "cos distance between Query and Nearest Incoming Link", FeatureMeta.ValueType.VEC);

    private double minTitle;
    private double minBody;
    private double minLink;

    @Override
    public void accept(QURLItem item) {
        super.accept(item);
        minTitle = 1;
        minBody = 1;
        minLink = 1;
    }

    @Override
    public Vec advance() {
        set(TITLE, minTitle);
        set(SECTION, minBody);
        set(LINK, minLink);
        return super.advance();
    }

    @Override
    public void withTitle(double distance) {
        minTitle = Math.min(minTitle, distance);
    }

    @Override
    public void withBody(double distance) {
        minBody = Math.min(minBody, distance);
    }

    @Override
    public void withLink(double distance) {
        minLink = Math.min(minLink, distance);
    }


}
