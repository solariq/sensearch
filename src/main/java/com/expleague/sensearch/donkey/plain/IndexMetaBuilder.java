package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import com.expleague.sensearch.donkey.utils.BrandNewIdGenerator;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;

public class IndexMetaBuilder {

  private static final Logger LOG = Logger.getLogger(IndexMetaBuilder.class);

  private final int version;

  private List<IndexMetaBuilderState> allStates = Collections.synchronizedList(new ArrayList<>());
  private ThreadLocal<IndexMetaBuilderState> localState =
      ThreadLocal.withInitial(
          () -> {
            IndexMetaBuilderState state = new IndexMetaBuilderState();
            allStates.add(state);
            return state;
          });

  public IndexMetaBuilder(int version) {
    this.version = version;
  }

  public void startPage(long pageId, int pageTitleWordCount) {
    IndexMetaBuilderState state = localState.get();
    if (state.isProcessingPage) {
      throw new IllegalStateException("Duplicate startPage call: page is already being processed");
    }
    state.isProcessingPage = true;
    state.pageCount++;

    state.pageIdToTitleSize.put(pageId, pageTitleWordCount);
  }

  public void addTerm(long termId, TermSegment termSegment) {
    IndexMetaBuilderState state = localState.get();
    state.termIds.add(termId);
    switch (termSegment) {
      case SECTION_TITLE:
        state.totalTokenCount++;
        state.titleTokensCount++;
      case TEXT:
        state.totalTokenCount++;
    }
  }

  public void addSection(long sectionId) {
    IndexMetaBuilderState state = localState.get();
    state.titlesCount++;
  }

  public void addLink(Link link) {
    localState
        .get()
        .incomingLinksCounts
        .adjustOrPutValue(BrandNewIdGenerator.generatePageId(link.targetUri()), 1, 1);
  }

  public void endPage() {
    IndexMetaBuilderState state = localState.get();
    if (!state.isProcessingPage) {
      throw new IllegalStateException("Illegal call to endPage: no page is being processed");
    }
    state.isProcessingPage = false;
  }

  public IndexMeta build() {
    LOG.info("Building index meta...");

    int titlesCount = allStates.stream().mapToInt(state -> state.titlesCount).sum();
    int pageCount = allStates.stream().mapToInt(state -> state.pageCount).sum();
    int titleTokensCount = allStates.stream().mapToInt(state -> state.titleTokensCount).sum();
    int totalTokenCount = allStates.stream().mapToInt(state -> state.totalTokenCount).sum();

    int[] linkTargetTitleTokensCount = new int[]{0};
    int[] linksCount = new int[]{0};

    allStates.forEach(
        state ->
            state.pageIdToTitleSize.forEachKey(
                id -> {
                  int incomingLinksCount =
                      state.incomingLinksCounts.containsKey(id)
                          ? state.incomingLinksCounts.get(id)
                          : 0;
                  linkTargetTitleTokensCount[0] +=
                      state.pageIdToTitleSize.get(id) * incomingLinksCount;
                  linksCount[0] += incomingLinksCount;
                  return true;
                }));

    TLongSet termIds = new TLongHashSet();
    allStates.forEach(
        state -> {
          termIds.addAll(state.termIds);
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

  private class IndexMetaBuilderState {

    int titleTokensCount = 0;
    int titlesCount = 0;
    int totalTokenCount = 0;
    int pageCount = 0;
    boolean isProcessingPage = false;
    final TLongSet termIds = new TLongHashSet();
    final TLongIntMap incomingLinksCounts = new TLongIntHashMap();
    final TLongIntMap pageIdToTitleSize = new TLongIntHashMap();
  }
}
