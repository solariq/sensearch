package com.expleague.sensearch.features.sets.ranker;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.commons.math.vectors.impl.vectors.ArrayVec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.index.plain.PlainPage;
import java.util.ArrayList;
import java.util.List;

public class CosDistanceFeatureSet extends FeatureSet.Stub<QURLItem> {

  private static final FeatureMeta COS_TITLE =
      FeatureMeta.create("cos-title", "cos distance between Query and Title", ValueType.VEC);
  private static final FeatureMeta COS_MIN_PASSAGE =
      FeatureMeta.create("cos-min-passage", "cos distance between Query and Body", ValueType.VEC);
  private static final FeatureMeta COS_MIN_FIVE_GRAM =
      FeatureMeta.create(
          "cos-min-five-gram", "cos distance between Query and five-grams", ValueType.VEC);

  private Page page;
  private Vec queryVec;
  private Vec titleVec;
  private List<Vec> termVecs;
  private final List<Vec> passages = new ArrayList<>();
  private int[] termDocFreqs;

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

  public void withTerms(List<Vec> termVecs, int[] termDocFreqs) {
    this.termVecs = termVecs;
    this.termDocFreqs = termDocFreqs;
  }

  @Override
  public Vec advance() {
    if (page == PlainPage.EMPTY_PAGE) {
      set(COS_TITLE, 1);
      set(COS_MIN_PASSAGE, 1);
      set(COS_MIN_FIVE_GRAM, 1);
      return super.advance();
    }

    Vec qNorm = VecTools.normalizeL2(VecTools.copy(queryVec));
    set(COS_TITLE, (1 - cosine(qNorm, titleVec)) / 2);

    double maxCos = -1.0;
    for (Vec passage : passages) {
      maxCos = Math.max(maxCos, cosine(qNorm, passage));
    }

    set(COS_MIN_PASSAGE, (1 - maxCos) / 2);

    double maxFiveGramCos = -1.0;
    if (termVecs.size() >= 5) {
      Vec curVec = new ArrayVec(queryVec.dim());
      for (int i = 0; i < 4; i++) {
        VecTools.incscale(curVec, termVecs.get(i), 1.0 / Math.log(1.0 * (1 + termDocFreqs[i])));
      }

      for (int i = 4; i < termVecs.size(); i++) {
        VecTools.incscale(curVec, termVecs.get(i), 1.0 / Math.log(1.0 * (1 + termDocFreqs[i])));
        maxFiveGramCos = Math.max(maxFiveGramCos, cosine(qNorm, curVec));
        VecTools.incscale(
            curVec, termVecs.get(i - 4), -1.0 / Math.log(1.0 * (1 + termDocFreqs[i - 4])));
      }
    }
    set(COS_MIN_FIVE_GRAM, maxFiveGramCos);

    return super.advance();
  }

  public static double cosine(final Vec qNorm, final Vec right) {
    final double scalarMultiplication = VecTools.multiply(qNorm, right);
    return scalarMultiplication != 0.0 ? scalarMultiplication / VecTools.norm(right) : 0.0;
  }
}
