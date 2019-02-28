package com.expleague.sensearch.features.sets.ranker;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.LinkType;
import com.expleague.sensearch.features.QURLItem;

public class LinkFeatureSet extends FeatureSet.Stub<QURLItem> {

  private final static FeatureMeta INCOMING_LINK = FeatureMeta
      .create("incoming-links", "incoming wiki-links", ValueType.VEC);
  private final static FeatureMeta OUTGOING_LINK = FeatureMeta
      .create("outgoing-links", "outgoing wiki-links", ValueType.VEC);

  private Page page;
  @Override
  public void accept(QURLItem item) {
    this.page = item.pageCache();
  }

  @Override
  public Vec advance() {
    set(INCOMING_LINK, page.incomingLinksCount(LinkType.ALL_LINKS));
    set(OUTGOING_LINK, page.outgoingLinksCount(LinkType.ALL_LINKS));
    return super.advance();
  }
}
