package com.expleague.sensearch.miner.features;

import com.expleague.commons.func.Functions;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.miner.features.TextFeatureSet.Segment;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AccumulatorFeatureSet extends FeatureSet.Stub<QURLItem> {

  private final Index index;
  private final FeatureSet<QURLItem> features = FeatureSet.join(
      new BM25FeatureSet(),
      new HHFeatureSet(),
      new LinkFeatureSet(),
      new CosDistanceFeatureSet(),
      new DocBasedFeatureSet(),
      new QuotationFeatureSet()
  );

  public AccumulatorFeatureSet(Index index) {
    this.index = index;
  }

  @Override
  public void accept(QURLItem item) {
    features.accept(item);
    final Page page = item.pageCache();
    { // Text features processing

      final int titleLength = (int) index.parse(page.content(SegmentType.SECTION_TITLE)).count();
      final int contentLength = (int) index.parse(page.content(SegmentType.BODY)).count();
      final int totalLength = titleLength + contentLength;

      features.components()
          .map(Functions.cast(TextFeatureSet.class))
          .filter(Objects::nonNull)
          .forEach(fs -> fs
              .withStats(totalLength, index.averageSectionTitleSize() + index.averagePageSize(),
                  titleLength, index.averageSectionTitleSize(),
              contentLength, index.averagePageSize(),
              index.size()));
      { // Title processing
        //features.components().map(Functions.cast(TextFeatureSet.class)).filter(Objects::nonNull)
        //    .forEach(fs -> fs.withSegment(TextFeatureSet.Segment.FULL_TITLE, titleLength));
        TermConsumer termConsumer = new TermConsumer();
        index.parse(page.content(SegmentType.SECTION_TITLE)).forEach(termConsumer);
        index.parse(page.content(SegmentType.SECTION_TITLE)).forEach(term -> features.components()
            .map(Functions.cast(TextFeatureSet.class))
            .filter(Objects::nonNull)
            .forEach(fs -> fs.withSegment(Segment.TITLE, term)));
      }
      { // Content processing
        //features.components().map(Functions.cast(TextFeatureSet.class)).filter(Objects::nonNull)
        //    .forEach(fs -> fs.withSegment(TextFeatureSet.Segment.BODY, contentLength));
        TermConsumer termConsumer = new TermConsumer();
        index.parse(page.content(SegmentType.BODY)).forEach(termConsumer);
        index.parse(page.content(SegmentType.BODY)).forEach(term -> features.components()
            .map(Functions.cast(TextFeatureSet.class))
            .filter(Objects::nonNull)
            .forEach(fs -> fs.withSegment(Segment.BODY, term)));
        index.parse(page.content(SegmentType.BODY)).forEach(term -> features.components()
            .map(Functions.cast(DocBasedFeatureSet.class))
            .filter(Objects::nonNull)
            .forEach(fs -> fs.withTerm(term)));

      }
    }
    { //Link Processing
    }
    { //Cos-Dist processing
      Vec queryVec = index.vecByTerms(item.queryCache().terms());
      Vec titleVec = index.vecByTerms(
          index.parse(item.pageCache().content(SegmentType.SECTION_TITLE))
              .collect(Collectors.toList()));

      features.components()
          .map(Functions.cast(CosDistanceFeatureSet.class))
          .filter(Objects::nonNull)
          .forEach(fs -> fs.withStats(queryVec, titleVec));

      item.pageCache().sentences(SegmentType.BODY).forEach(sent -> {
        Vec passageVec = index.vecByTerms(index.parse(sent).collect(Collectors.toList()));
        features.components()
            .map(Functions.cast(CosDistanceFeatureSet.class))
            .filter(Objects::nonNull)
            .forEach(fs -> fs.withPassage(passageVec));
      });
    }
    { //Quotation
      item.pageCache().sentences(SegmentType.BODY).forEach(sentence -> features.components()
          .map(Functions.cast(QuotationFeatureSet.class))
          .filter(Objects::nonNull)
          .forEach(fs -> fs.withPassage(index.parse(sentence).collect(Collectors.toList()))));
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

  @Override
  public FeatureMeta meta(int ind) {
    return features.meta(ind);
  }

  @Override
  public int index(FeatureMeta meta) {
    return features.index(meta);
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
