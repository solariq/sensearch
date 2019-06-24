package com.expleague.sensearch.features.sets.ranker;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.index.IndexedPage;
import com.expleague.sensearch.index.plain.PlainPage;
import java.util.List;
import java.util.stream.Collectors;

public class DocBasedFeatureSet extends FeatureSet.Stub<QURLItem> {

  private static final FeatureMeta PASSAGES_COUNT =
      FeatureMeta.create("passages-count", "Passages in document", ValueType.VEC);
  private static final FeatureMeta SENTENCE_LEVEL =
      FeatureMeta.create("sentence-level", "non interrogative sentences", ValueType.VEC);
  private static final FeatureMeta NOUN_COUNT =
      FeatureMeta.create("noun-count", "Nouns in document", ValueType.VEC);

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

  private boolean isInterrogative(List<Term> s) {
    return s.get(s.size() - 1).text() == "?";
  }

  @Override
  public Vec advance() {
    // FIXME: do NOT cast page down to 'PlainPage'
    List<List<Term>> sentencesList = ((PlainPage) page).sentences(true, SegmentType.BODY)
        .collect(Collectors.toList());
    long sentences = sentencesList.size();
    set(PASSAGES_COUNT, (double) sentences);
    set(
        SENTENCE_LEVEL,
        sentences == 0
            ? 0.0
            : (double) sentencesList.stream().filter(s -> !isInterrogative(s)).count() / sentences);
    set(NOUN_COUNT, terms == 0 ? 0 : (double) nouns / terms);
    return super.advance();
  }
}
