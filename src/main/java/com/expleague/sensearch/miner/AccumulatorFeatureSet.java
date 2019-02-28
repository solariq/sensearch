package com.expleague.sensearch.miner;

import com.expleague.commons.func.Functions;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.features.QURLItem;
import com.expleague.sensearch.features.sets.ranker.BM25FeatureSet;
import com.expleague.sensearch.features.sets.ranker.CosDistanceFeatureSet;
import com.expleague.sensearch.features.sets.ranker.DocBasedFeatureSet;
import com.expleague.sensearch.features.sets.ranker.HHFeatureSet;
import com.expleague.sensearch.features.sets.ranker.LinkFeatureSet;
import com.expleague.sensearch.features.sets.ranker.QuotationFeatureSet;
import com.expleague.sensearch.features.sets.ranker.TextFeatureSet;
import com.expleague.sensearch.features.sets.ranker.TextFeatureSet.Segment;
import com.expleague.sensearch.index.Index;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AccumulatorFeatureSet extends FeatureSet.Stub<QURLItem> {

  private final Index index;
  private final FeatureSet<QURLItem> features =
      FeatureSet.join(
          new BM25FeatureSet(),
          new HHFeatureSet(),
          new LinkFeatureSet(),
          new CosDistanceFeatureSet(),
          new DocBasedFeatureSet(),
          new QuotationFeatureSet()
          //      ,
          //      new FilterFeatures()
      );
  private Map<Page, Features> filterFeatures;

  public AccumulatorFeatureSet(Index index) {
    this.index = index;
  }

  void acceptFilterFeatures(Map<Page, Features> filterFeatures) {
    this.filterFeatures = filterFeatures;
  }

  @Override
  public void accept(QURLItem item) {
    features.accept(item);
    final Page page = item.pageCache();

    CharSequence body = page.content(SegmentType.BODY);
    CharSequence title = page.content(SegmentType.SECTION_TITLE);

    List<Term> titleTerms = index.parse(title).collect(Collectors.toList());
    List<Term> bodyTerms = index.parse(body).collect(Collectors.toList());

    { // Text features processing
      final int titleLength = titleTerms.size();
      final int contentLength = bodyTerms.size();
      final int totalLength = titleLength + contentLength;

      features
          .components()
          .map(Functions.cast(TextFeatureSet.class))
          .filter(Objects::nonNull)
          .forEach(
              fs ->
                  fs.withStats(
                      totalLength,
                      index.averageSectionTitleSize() + index.averagePageSize(),
                      titleLength,
                      index.averageSectionTitleSize(),
                      contentLength,
                      index.averagePageSize(),
                      index.size()));
      { // Title processing
        // features.components().map(Functions.cast(TextFeatureSet.class)).filter(Objects::nonNull)
        //    .forEach(fs -> fs.withSegment(TextFeatureSet.Segment.FULL_TITLE, titleLength));
        TermConsumer termConsumer = new TermConsumer();
        titleTerms.forEach(termConsumer);
        titleTerms.forEach(
            term ->
                features
                    .components()
                    .map(Functions.cast(TextFeatureSet.class))
                    .filter(Objects::nonNull)
                    .forEach(fs -> fs.withSegment(Segment.TITLE, term)));
      }
      { // Content processing
        // features.components().map(Functions.cast(TextFeatureSet.class)).filter(Objects::nonNull)
        //    .forEach(fs -> fs.withSegment(TextFeatureSet.Segment.BODY, contentLength));
        TermConsumer termConsumer = new TermConsumer();
        bodyTerms.forEach(termConsumer);
        bodyTerms.forEach(
            term ->
                features
                    .components()
                    .map(Functions.cast(TextFeatureSet.class))
                    .filter(Objects::nonNull)
                    .forEach(fs -> fs.withSegment(Segment.BODY, term)));
        bodyTerms.forEach(
            term ->
                features
                    .components()
                    .map(Functions.cast(DocBasedFeatureSet.class))
                    .filter(Objects::nonNull)
                    .forEach(fs -> fs.withTerm(term)));
      }
    }
    { // Link Processing
    }
    //    { //Filter Features Processing
    //      features.components()
    //          .map(Functions.cast(FilterFeatures.class))
    //          .filter(Objects::nonNull)
    //          .forEach(fs -> {
    //            Vec ff = filterFeatures.get(page).features();
    //            fs.withBody(ff.get(1));
    //            fs.withTitle(ff.get(0));
    //            fs.withLink(ff.get(2));
    //          });
    //    }
    List<List<Term>> pageSentenceTokens =
        page.sentences(SegmentType.BODY)
            .map(sentence -> index.parse(sentence).collect(Collectors.toList()))
            .collect(Collectors.toList());
    { // Cos-Dist processing
      Vec queryVec = index.vecByTerms(item.queryCache().terms());
      Vec titleVec = index.vecByTerms(titleTerms);

      features
          .components()
          .map(Functions.cast(CosDistanceFeatureSet.class))
          .filter(Objects::nonNull)
          .forEach(fs -> fs.withStats(queryVec, titleVec));

      pageSentenceTokens.forEach(
          sentenceTokens -> {
            Vec passageVec = index.vecByTerms(sentenceTokens);
            features
                .components()
                .map(Functions.cast(CosDistanceFeatureSet.class))
                .filter(Objects::nonNull)
                .forEach(fs -> fs.withPassage(passageVec));
          });
    }
    { // Quotation
      pageSentenceTokens.forEach(
          sentenceTokens ->
              features
                  .components()
                  .map(Functions.cast(QuotationFeatureSet.class))
                  .filter(Objects::nonNull)
                  .forEach(fs -> fs.withPassage(sentenceTokens)));
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
      features
          .components()
          .map(Functions.cast(TextFeatureSet.class))
          .filter(Objects::nonNull)
          .forEach(fs -> fs.withTerm(term, index));
      index++;
    }
  }
}
