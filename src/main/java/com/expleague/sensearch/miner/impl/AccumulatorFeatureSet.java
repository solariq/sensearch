package com.expleague.sensearch.miner.impl;

import com.expleague.commons.func.Functions;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import java.util.Objects;
import java.util.function.Consumer;

public class AccumulatorFeatureSet extends FeatureSet.Stub<QURLItem> {
  private final Index index;
  private final FeatureSet<QURLItem> features = FeatureSet.join(
      new BM25FeatureSet()
  );

  public AccumulatorFeatureSet(Index index) {
    this.index = index;
  }

  @Override
  public void accept(QURLItem item) {
    features.accept(item);
    final Page page = item.pageCache();
    { // Text features processing

      final int titleLength = (int) index.parse(page.title()).count();
      final int contentLength = (int) index.parse(page.fullContent()).count();
      final int totalLength = titleLength + contentLength;

      features.components()
          .map(Functions.cast(TextFeatureSet.class))
          .filter(Objects::nonNull)
          .forEach(fs -> fs.withStats(totalLength, index.averagePageSize(), index.size()));
      { // Title processing
        features.components().map(Functions.cast(TextFeatureSet.class)).filter(Objects::nonNull)
            .forEach(fs -> fs.withSegment(TextFeatureSet.Segment.TITLE, titleLength));
        index.parse(page.title()).forEach(new TermConsumer());
      }
      { // Content processing
        features.components().map(Functions.cast(TextFeatureSet.class)).filter(Objects::nonNull)
            .forEach(fs -> fs.withSegment(TextFeatureSet.Segment.BODY, contentLength));
        index.parse(page.fullContent()).forEach(new TermConsumer());
      }
    }
  }

  @Override
  public Vec advanceTo(Vec to) {
    return features.advanceTo(to);
  }

  @Override
  public int dim() {
    return features.dim();
  }

  private class TermConsumer implements Consumer<Term> {
    int index = 0;

    TermConsumer() {
    }

    @Override
    public void accept(Term term) {
      features.components()
          .map(Functions.cast(TextFeatureSet.class))
          .filter(Objects::nonNull)
          .forEach(fs -> fs.withTerm(term, index));
      index++;
    }
  }
}
