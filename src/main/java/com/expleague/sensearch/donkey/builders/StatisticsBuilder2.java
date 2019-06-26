package com.expleague.sensearch.donkey.builders;

import com.expleague.sensearch.donkey.randomaccess.RandomAccess;
import com.expleague.sensearch.donkey.utils.SerializedTextHelperFactory;
import com.expleague.sensearch.donkey.utils.SerializedTextHelperFactory.SerializedTextHelper;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.protobuf.index.IndexUnits.Term;
import com.google.common.annotations.VisibleForTesting;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

// fixme: used this name to avoid conflict with the existing builder
// rename when old one will be deleted
public class StatisticsBuilder2 {
  private final TLongObjectMap<StatisticsAccumulator> pageStatsAccumulators;
  private final SerializedTextHelperFactory helperFactory;
  public StatisticsBuilder2(RandomAccess<Term> termIndex) {
    pageStatsAccumulators = new TLongObjectHashMap<>();
    helperFactory = new SerializedTextHelperFactory(termIndex);
  }

  public void addPage(Page page) {
    long rootId = page.getRootId();
    pageStatsAccumulators.putIfAbsent(rootId, new StatisticsAccumulator(rootId));
    pageStatsAccumulators.get(rootId).accumulate(page);
  }

  public synchronized void build() {
    // TODO: implement!
  }

  @VisibleForTesting
  class StatisticsAccumulator {
    // index-wise statistics
    private int titlesCount;
    private int titleTokensCount;
    private int contentTokensCount;
    private int linkTokensCount;
    private int incomingLinksCount;

    // term-wise statistics
    private final TIntLongMap wordFrequencyMap = new TIntLongHashMap();

    private final long accumulatorId;
    StatisticsAccumulator(long accumulatorId) {
      this.accumulatorId = accumulatorId;
    }

    synchronized void accumulate(Page page) {
      if (page.getRootId() != accumulatorId) {
        return;
      }

      incomingLinksCount += page.getIncomingLinksCount();
      titlesCount++;

      SerializedTextHelper titleHelper = helperFactory.helper(page.getTitle());
      titleHelper.lemmaIdsStream().forEach(id -> wordFrequencyMap.adjustOrPutValue(id, 1, 1));
      titleTokensCount += (int) titleHelper.termIdsStream()
          .peek(id -> wordFrequencyMap.adjustOrPutValue(id, 1, 1))
          .count();


      SerializedTextHelper contentHelper = helperFactory.helper(page.getContent());
      contentHelper.lemmaIdsStream().forEach(id -> wordFrequencyMap.adjustOrPutValue(id, 1, 1));
      contentTokensCount += (int) contentHelper.termIdsStream()
          .peek(id -> wordFrequencyMap.adjustOrPutValue(id, 1, 1))
          .count();

      linkTokensCount += page.getIncomingLinksList().stream()
          .flatMapToInt(l -> l.getText().getTokenIdsList().stream().mapToInt(Integer::intValue))
          .count();
    }

    public int titlesCount() {
      return titlesCount;
    }

    public int titleTokensCount() {
      return titleTokensCount;
    }

    public int contentTokensCount() {
      return contentTokensCount;
    }

    public int linkTokensCount() {
      return linkTokensCount;
    }

    public int totalTokensCount() {
      return titleTokensCount + contentTokensCount;
    }

    public int incomingLinksCount() {
      return incomingLinksCount;
    }

    public TIntLongMap wordFrequencyMap() {
      return wordFrequencyMap;
    }

    public long accumulatorId() {
      return accumulatorId;
    }
  }

}
