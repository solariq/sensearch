package com.expleague.sensearch.miner.impl;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.Term;

public class DocBasedFeatureSet extends FeatureSet.Stub<QURLItem> {
  public final static FeatureMeta PASSAGES_COUNT = FeatureMeta.create("passages-count","Passages in document", ValueType.VEC);
  public final static FeatureMeta SENTENCE_LEVEL = FeatureMeta.create("sentence-level","non interrogative sentences", ValueType.VEC);
  public final static FeatureMeta NOUN_COUNT = FeatureMeta.create("noun-count","Nouns in document", ValueType.VEC);

  private Page page;


  private int nouns, terms;
  public void accept(QURLItem item) {

    this.page = item.pageCache();
    nouns = 0;
    terms = 0;
  }


  public void withTerm(Term t) {
    terms++;
    if (t.partOfSpeech() == PartOfSpeech.S) {
      nouns++;
    }
  }

  private boolean isInterrogative(CharSequence s) {
    return s.charAt(s.length() - 1) == '?';
  }

  @Override
  public Vec advance() {
    long sentences = page.sentences(SegmentType.BODY).count();
    set(PASSAGES_COUNT, (double) sentences);
    set(SENTENCE_LEVEL, sentences == 0 ? 0.0 : (double) page.sentences(SegmentType.BODY).filter(s -> !isInterrogative(s)).count() / sentences);
    set(NOUN_COUNT, terms == 0 ? 0 : (double) nouns / terms);
    return super.advance();
  }
}
