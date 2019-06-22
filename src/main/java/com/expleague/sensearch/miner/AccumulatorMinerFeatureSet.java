package com.expleague.sensearch.miner;

import com.expleague.commons.func.Functions;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.ml.data.tools.FeatureSet;
import com.expleague.ml.meta.FeatureMeta;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.core.Term;
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
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AccumulatorMinerFeatureSet extends FeatureSet.Stub<QURLItem> {

  private final Index index;

  private final BM25FeatureSet bm25FeatureSet = new BM25FeatureSet();
  private final HHFeatureSet hhFeatureSet = new HHFeatureSet();
  private final LinkFeatureSet linkFeatureSet = new LinkFeatureSet();
  private final CosDistanceFeatureSet cosDistanceFeatureSet = new CosDistanceFeatureSet();
  private final DocBasedFeatureSet docBasedFeatureSet = new DocBasedFeatureSet();
  private final QuotationFeatureSet quotationFeatureSet = new QuotationFeatureSet();

  private final FeatureSet<QURLItem> features =
          FeatureSet.join(
                  bm25FeatureSet,
                  hhFeatureSet,
                  linkFeatureSet,
                  cosDistanceFeatureSet,
                  docBasedFeatureSet,
                  quotationFeatureSet
          );

  private final List<TextFeatureSet> textFeatureSet =
          features
                  .components()
                  .map(Functions.cast(TextFeatureSet.class))
                  .filter(Objects::nonNull)
                  .collect(Collectors.toList());


  public AccumulatorMinerFeatureSet(Index index) {
    this.index = index;
  }


  @Override
  public void accept(QURLItem item) {
    features.accept(item);
    final Page page = item.pageCache();

//    CharSequence body = page.content(SegmentType.BODY);
//    CharSequence title = page.content(SegmentType.SECTION_TITLE);

//    List<Term> titleTerms = index.parse(title).collect(Collectors.toList());
    List<Term> titleTerms = page.content(false, SegmentType.SECTION_TITLE).collect(Collectors.toList());
//    List<Term> bodyTerms = index.parse(body).collect(Collectors.toList());
    List<Term> bodyTerms = page.content(false, SegmentType.BODY).collect(Collectors.toList());

    { // Text features processing
      final int titleLength = titleTerms.size();
      final int contentLength = bodyTerms.size();
      final int totalLength = titleLength + contentLength;

      textFeatureSet.forEach(
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
                term -> textFeatureSet.forEach(fs -> fs.withSegment(Segment.TITLE, term)));
      }
      { // Content processing
        // features.components().map(Functions.cast(TextFeatureSet.class)).filter(Objects::nonNull)
        //    .forEach(fs -> fs.withSegment(TextFeatureSet.Segment.BODY, contentLength));
        TermConsumer termConsumer = new TermConsumer();
        bodyTerms.forEach(termConsumer);
        bodyTerms.forEach(term -> textFeatureSet.forEach(fs -> fs.withSegment(Segment.BODY, term)));
        bodyTerms.forEach(docBasedFeatureSet::withTerm);
      }
    }
    { // Link Processing
    }
    { // Cos-Dist processing
      Vec queryVec = item.queryCache().vec();
      Vec titleVec = page.titleVec();
      cosDistanceFeatureSet.withStats(queryVec, titleVec);
      page.sentenceVecs().forEach(cosDistanceFeatureSet::withPassage);
    }
    { // Quotation
      page.sentences(SegmentType.BODY)
//              .map(sentence -> index.parse(sentence).collect(Collectors.toList()))
              .forEach(quotationFeatureSet::withPassage);
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
      textFeatureSet.forEach(fs -> fs.withTerm(term, index));
      index++;
    }
  }
}
