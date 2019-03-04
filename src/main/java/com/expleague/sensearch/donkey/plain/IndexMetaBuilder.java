package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import com.expleague.sensearch.donkey.utils.BrandNewIdGenerator;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.apache.log4j.Logger;

public class IndexMetaBuilder {

  private static final Logger LOG = Logger.getLogger(IndexMetaBuilder.class);

  private final int version;
  private final TLongSet termIds = new TLongHashSet();
  private final TLongIntMap incomingLinksCounts = new TLongIntHashMap();

  private final TLongIntMap pageIdToTitleSize = new TLongIntHashMap();

  private int titleTokensCount = 0;
  private int titlesCount = 0;
  private int totalTokenCount = 0;
  private int pageCount = 0;
  private boolean isProcessingPage = false;

  public IndexMetaBuilder(int version) {
    this.version = version;
  }

  public void startPage(long pageId, int pageTitleWordCount) {
    if (isProcessingPage) {
      throw new IllegalStateException("Duplicate startPage call: page is already being processed");
    }
    isProcessingPage = true;
    pageCount++;

    pageIdToTitleSize.put(pageId, pageTitleWordCount);
  }

  public void addTerm(long termId, TermSegment termSegment) {
    termIds.add(termId);
    switch (termSegment) {
      case SECTION_TITLE:
        totalTokenCount++;
        titleTokensCount++;
      case TEXT:
        totalTokenCount++;
    }
  }

  public void addSection(long sectionId) {
    ++titlesCount;
  }

  public void addLink(Link link) {
    incomingLinksCounts.adjustOrPutValue(
        BrandNewIdGenerator.pageIdGenerator(link.targetUri()).next(), 1, 1);
  }

  public void endPage() {
    if (!isProcessingPage) {
      throw new IllegalStateException("Illegal call to endPage: no page is being processed");
    }
    isProcessingPage = false;
  }

  public IndexMeta build() {

    int[] linkTargetTitleTokensCount = new int[]{0};
    int[] linksCount = new int[]{0};
    pageIdToTitleSize.forEachKey(
        id -> {
          int incomingLinksCount =
              incomingLinksCounts.containsKey(id) ? incomingLinksCounts.get(id) : 0;
          linkTargetTitleTokensCount[0] += pageIdToTitleSize.get(id) * incomingLinksCount;
          linksCount[0] += incomingLinksCount;
          return true;
        });

    return IndexMeta.newBuilder()
        .setVersion(version)
        .setLinksCount(linksCount[0])
        .setAverageLinkTargetTitleWordCount((double) linkTargetTitleTokensCount[0] / linksCount[0])
        .setSectionTitlesCount(titlesCount)
        .setAverageSectionTitleSize((double) titleTokensCount / titlesCount)
        .setAveragePageSize((double) totalTokenCount / pageCount)
        .setPagesCount(pageCount)
        .setVocabularySize(termIds.size())
        .build();
  }

  public enum TermSegment {
    SECTION_TITLE,
    TEXT
  }
}
