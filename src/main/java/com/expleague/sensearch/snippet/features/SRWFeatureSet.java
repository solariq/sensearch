package com.expleague.sensearch.snippet.features;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.ml.meta.FeatureMeta.ValueType;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.snippet.docbased_snippet.KeyWord;
import com.expleague.sensearch.snippet.passage.Passage;
import java.util.List;

public class SRWFeatureSet extends FeatureSet.Stub<QPASItem> {

  private static final FeatureMeta SRW = FeatureMeta
      .create("sr-weight", "Statistical Relevance Weight", ValueType.VEC);

  private Passage passage;
  private List<KeyWord> keywords;

  @Override
  public void accept(QPASItem item) {
    this.passage = item.passageCache();
    super.accept(item);
  }

  public void withKeyWords(List<KeyWord> keyWords) {
    this.keywords = keyWords;
  }

  private static boolean containsWithLemma(Passage passage, Term term) {
    return passage.words().anyMatch(x -> x.lemma() == term.lemma());
  }

  private static boolean contains(Passage passage, Term term) {
    return passage.words().anyMatch(x -> x == term);
  }

  @Override
  public Vec advance() {
    double sum = keywords
        .stream()
        .filter(keyWord -> containsWithLemma(passage, keyWord.word()))
        .mapToDouble(KeyWord::rank)
        .sum();

    set(SRW, sum);

    return super.advance();
  }
}
