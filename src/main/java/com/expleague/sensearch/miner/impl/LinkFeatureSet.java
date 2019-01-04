package com.expleague.sensearch.miner.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.Page;

public class LinkFeatureSet extends FeatureSet.Stub<QURLItem> {
  public final static FeatureMeta INCOMING_LINK = FeatureMeta
      .create("incoming links", "incoming wiki-links", ValueType.VEC);

  public final static FeatureMeta OUTCOMING_LINK = FeatureMeta
      .create("outcoming links", "outcoming wiki-links", ValueType.VEC);

  private Page page;
  private long indexSize;

  public void withIndexSize(long indexSize) {
    this.indexSize = indexSize;
  }

  @Override
  public void accept(QURLItem item) {
    this.page = item.pageCache();
  }

  @Override
  public Vec advance() {
    set(INCOMING_LINK, page.incomingLinks().count() / (double)indexSize);
    set(OUTCOMING_LINK, page.outcomingLinks().count() / (double)indexSize);
    return super.advance();
  }
}
