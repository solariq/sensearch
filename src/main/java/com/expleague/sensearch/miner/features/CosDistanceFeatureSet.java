package com.expleague.sensearch.miner.features;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.index.plain.PlainPage;
import java.util.ArrayList;
import java.util.List;

public class CosDistanceFeatureSet extends FeatureSet.Stub<QURLItem> {

  private final static FeatureMeta COS_TITLE = FeatureMeta
      .create("cos-title", "cos distance between Query and Title", ValueType.VEC);
  private final static FeatureMeta COS_MIN_PASSAGE = FeatureMeta
      .create("cos-min-passage", "cos distance between Query and Body", ValueType.VEC);



  private Page page;
  private Vec queryVec;
  private Vec titleVec;
  private final List<Vec> passages = new ArrayList<>();

  @Override
  public void accept(QURLItem item) {
    this.page = item.pageCache();
    super.accept(item);
    passages.clear();
  }

  public void withPassage(Vec passage) {
    passages.add(passage);
  }

  public void withStats(Vec queryVec, Vec titleVec) {
    this.queryVec = queryVec;
    this.titleVec = titleVec;
  }

  @Override
  public Vec advance() {
    if (page == PlainPage.EMPTY_PAGE) {
      set(COS_TITLE, 1);
      set(COS_MIN_PASSAGE, 1);
      return super.advance();
    }
    set(COS_TITLE, (1 - VecTools.cosine(queryVec, titleVec)) / 2);

    double maxCos = -1.0;
    for (Vec passage : passages) {
      maxCos = Math.max(maxCos, VecTools.cosine(queryVec, passage));
    }
    set(COS_MIN_PASSAGE, (1 - maxCos) / 2);
    return super.advance();
  }
}
