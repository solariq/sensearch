package com.expleague.sensearch.donkey.builders;

import com.expleague.sensearch.protobuf.index.IndexUnits.PageStatistics;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class IndexStatisticsBuilder {

  private int pagesCount;
  private int titlesCount;
  private int titleTermsCount;
  private int contentTermsCount;
  private int linkTermsCount;

  private int outgoingLinksCount;
  // FIXME
  // in fact this statistics will not have the same meaning as for
  // a single page since sum of unique targets counts for several pages is not
  // the same as actual number of unique targets for these pages:
  // they might have common target pages
  // On the other hand, we probably don't even need the number of referred pages.
  // Average number of unique target is probably enough
  private int uniqueTargetsCount;
  private int targetTitleTermsCount;

  private int incomingLinksCount;
  // FIXME
  // The same as for unique targets count
  private int uniqueReferrersCount;

  private TIntLongMap wordFrequencyMap = new TIntLongHashMap();
  private TIntIntMap documentFrequencyMap = new TIntIntHashMap();
  private TIntObjectMap<TIntLongMap> bigramFrequencyMap = new TIntObjectHashMap<>();

  public IndexStatisticsBuilder() {

  }

  public void addPageStatistics(PageStatistics pageStatistics) {
    pagesCount++;
    titlesCount += pageStatistics.getTitlesCount();
    titleTermsCount += pageStatistics.getTitleTermsCount();
    contentTermsCount += pageStatistics.getContentTermsCount();
    linkTermsCount += pageStatistics.getLinkTermsCount();

    outgoingLinksCount += pageStatistics.getOutgoingLinksCount();
    uniqueTargetsCount += pageStatistics.getUniqueTargetsCount();
    targetTitleTermsCount += pageStatistics.getTargetTitleTermsCount();

    incomingLinksCount += pageStatistics.getIncomingLinksCount();
    uniqueReferrersCount += pageStatistics.getUniqueReferrersCount();

    processUnigrams(pageStatistics);
    processBigrams(pageStatistics);
  }

  private void processUnigrams(PageStatistics pageStatistics) {
    pageStatistics.getUnigramsList().stream()
        .peek(vgf ->
            wordFrequencyMap.adjustOrPutValue(vgf.getTermSequence(0),
                vgf.getSequenceFrequency(),
                vgf.getSequenceFrequency())
        )
        .mapToInt(vgf -> vgf.getTermSequence(0))
        .distinct()
        .forEach(id -> documentFrequencyMap.adjustOrPutValue(id, 1, 1));
  }

  private void processBigrams(PageStatistics pageStatistics) {
    pageStatistics.getBigramsList().forEach(
        vgf -> {
          int firstWordId = vgf.getTermSequence(0);
          int secondWordId = vgf.getTermSequence(1);
          long frequency = vgf.getSequenceFrequency();

          bigramFrequencyMap
              .putIfAbsent(firstWordId, new TIntLongHashMap());
          bigramFrequencyMap
              .get(firstWordId)
              .adjustOrPutValue(secondWordId, frequency, frequency);
        }
    );
  }

  public void build() {

  }
}
