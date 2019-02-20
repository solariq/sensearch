package com.expleague.sensearch.snippet.features;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.snippet.passage.Passage;

public class PassageBasedFeatureSet extends FeatureSet.Stub<QPASItem> {

  private static final FeatureMeta WORDS_COUNT =
      FeatureMeta.create("words-count", "Words count in passage", ValueType.VEC);
  private static final FeatureMeta AFFIRMATIVE =
      FeatureMeta.create("affirmative", "passage is affirmative", ValueType.VEC);
  private static final FeatureMeta POSITION =
      FeatureMeta.create("position", "Position in document", ValueType.VEC);

  private Passage passage;

  @Override
  public void accept(QPASItem item) {
    passage = item.passageCache();
  }

  @Override
  public Vec advance() {
    set(WORDS_COUNT, passage.words().count());
    set(AFFIRMATIVE, isInterrogative(passage.sentence()) ? 0 : 1);
    set(POSITION, passage.id());
    return super.advance();
  }

  private boolean isInterrogative(CharSequence s) {
    return s.charAt(s.length() - 1) == '?';
  }

}
