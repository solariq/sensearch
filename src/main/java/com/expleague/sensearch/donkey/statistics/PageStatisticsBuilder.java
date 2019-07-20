package com.expleague.sensearch.donkey.statistics;

import com.expleague.sensearch.donkey.utils.SerializedTextHelperFactory;
import com.expleague.sensearch.donkey.utils.SerializedTextHelperFactory.SerializedTextHelper;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page.Link;
import com.expleague.sensearch.protobuf.index.IndexUnits.PageStatistics;
import com.expleague.sensearch.protobuf.index.IndexUnits.VgramFrequency;
import com.google.common.annotations.VisibleForTesting;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@VisibleForTesting
public class PageStatisticsBuilder {

  // index-wise statistics
  private int titlesCount;
  private int titleTermsCount;
  private int contentTermsCount;
  private int linkTermsCount;
  private int incomingLinksCount;
  private int outgoingLinksCount;
  private int targetTitleTermsCount;

  // term-wise statistics
  private final TIntLongMap wordFrequencyMap = new TIntLongHashMap();
  private final BigramsFreqMapAccumulator bigramFreqAcc = new BigramsFreqMapAccumulator();

  private final TLongSet knownTargets = new TLongHashSet();
  private final TLongIntMap uniqueTargets = new TLongIntHashMap();
  private final TLongIntMap uniqueReferrers = new TLongIntHashMap();

  private final SerializedTextHelperFactory textHelperFactory;
  private final long pageId;

  public PageStatisticsBuilder(SerializedTextHelperFactory textHelperFactory, long pageId) {
    this.textHelperFactory = textHelperFactory;
    this.pageId = pageId;
  }

  public synchronized void addTarget(Page page) {
    // this condition is always true for wikipedia but
    // we want to determine the same behaviour for all possible collection
    // not only for wikipedia
    if (page.getRootId() != page.getPageId()) {
      return;
    }

    if (knownTargets.contains(page.getPageId())) {
      return;
    }

    if (page.getIncomingLinksList().stream().anyMatch(l -> l.getSourcePageId() == pageId)) {
      targetTitleTermsCount += uniqueTargets.get(page.getPageId()) *
          (int) textHelperFactory.helper(page.getTitle())
              .termIdsStream()
              .count();
    }
  }

  public synchronized void addPage(Page page) {
    if (page.getRootId() != pageId) {
      return;
    }

    incomingLinksCount += (int) page.getIncomingLinksList().stream()
        .mapToLong(Link::getSourcePageId)
        .peek(r -> uniqueReferrers.adjustOrPutValue(r, 1, 1))
        .count();
    outgoingLinksCount += (int) page.getOutgoingLinksList().stream()
        .mapToLong(Link::getTargetPageId)
        .peek(t -> uniqueTargets.adjustOrPutValue(t, 1, 1))
        .count();
    titlesCount++;

    SerializedTextHelper titleHelper = textHelperFactory.helper(page.getTitle());
    processWordStream(titleHelper.lemmaIdsStream());
    titleTermsCount += processWordStream(titleHelper.termIdsStream());

    SerializedTextHelper contentHelper = textHelperFactory.helper(page.getContent());
    processWordStream(contentHelper.lemmaIdsStream());
    contentTermsCount += processWordStream(contentHelper.termIdsStream());

    linkTermsCount += page.getIncomingLinksList().stream()
        .flatMapToInt(l -> textHelperFactory.helper(l.getText()).termIdsStream())
        .count();
  }

  /**
   * Calculates word and bigram frequencies
   *
   * @param wordStream stream of word ids to be processed
   * @return count of the elements of the stream
   */
  private int processWordStream(IntStream wordStream) {
    int wordCount = (int) wordStream.sequential()
        .peek(bigramFreqAcc::accumulate)
        .peek(id -> wordFrequencyMap.adjustOrPutValue(id, 1, 1))
        .count();
    bigramFreqAcc.skipPreviousWord();

    return wordCount;
  }

  public PageStatistics build() {
    PageStatistics.Builder builder = PageStatistics.newBuilder();
    builder.setPageId(pageId);

    builder.setTitlesCount(titlesCount);
    builder.setTitleTermsCount(titleTermsCount);
    builder.setContentTermsCount(contentTermsCount);

    builder.setLinkTermsCount(linkTermsCount);

    builder.setIncomingLinksCount(incomingLinksCount);
    builder.setTargetTitleTermsCount(targetTitleTermsCount);
    builder.setOutgoingLinksCount(outgoingLinksCount);

    builder.setUniqueReferrersCount(uniqueReferrers.size());
    builder.setUniqueTargetsCount(uniqueTargets.size());

    builder.addAllUnigrams(unigrams());
    builder.addAllBigrams(bigrams());

    return builder.build();
  }

  private Iterable<VgramFrequency> unigrams() {
    List<VgramFrequency> unigrams = new ArrayList<>();
    wordFrequencyMap.forEachEntry(
        (id, f) -> unigrams.add(
            VgramFrequency.newBuilder()
                .addTermSequence(id)
                .setSequenceFrequency(f)
                .build()));
    return unigrams;
  }

  private Iterable<VgramFrequency> bigrams() {
    List<VgramFrequency> bigrams = new ArrayList<>();
    bigramFreqAcc.bigramsFrequencyMap().forEachEntry(
        (idW1, fmap) -> fmap.forEachEntry((idW2, f) -> bigrams.add(
            VgramFrequency.newBuilder()
                .addTermSequence(idW1)
                .addTermSequence(idW2)
                .setSequenceFrequency(f)
                .build()))
    );
    return bigrams;
  }

  static class BigramsFreqMapAccumulator {

    private int previousWordId;
    private boolean hasPreviousWord;

    private final TIntObjectMap<TIntIntMap> bigramsFrequencyMap;

    BigramsFreqMapAccumulator(TIntObjectMap<TIntIntMap> bigramsFrequencyMap) {
      this.bigramsFrequencyMap = bigramsFrequencyMap;
    }

    BigramsFreqMapAccumulator() {
      this(new TIntObjectHashMap<>());
    }

    void accumulate(int wordId) {
      if (!hasPreviousWord) {
        hasPreviousWord = true;
        previousWordId = wordId;
        return;
      }

      bigramsFrequencyMap.putIfAbsent(previousWordId, new TIntIntHashMap());
      bigramsFrequencyMap.get(previousWordId).adjustOrPutValue(wordId, 1, 1);
      previousWordId = wordId;
    }

    void skipPreviousWord() {
      hasPreviousWord = false;
    }

    TIntObjectMap<TIntIntMap> bigramsFrequencyMap() {
      return bigramsFrequencyMap;
    }
  }
}
