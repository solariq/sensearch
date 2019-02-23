package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.core.Tokenizer;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Section;
import com.expleague.sensearch.donkey.utils.BrandNewIdGenerator;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta.UriPageMapping;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

public class IndexMetaBuilder {

  private static final Logger LOG = Logger.getLogger(IndexMetaBuilder.class);

  private final int version;
  private final TLongSet termIds = new TLongHashSet();
  private final TLongSet knownOriginalIds = new TLongHashSet();
  private final TLongIntMap incomingLinksCounts = new TLongIntHashMap();

  private final TObjectLongMap<String> pageUri = new TObjectLongHashMap<>();
  private final Tokenizer tokenizer;

  private final TLongIntMap wikiIdToTitleSize = new TLongIntHashMap();

  private int titlesCount = 0;
  private int titleTokensCount = 0;

  private int totalTokenCount = 0;
  private boolean isProcessingPage = false;

  public IndexMetaBuilder(int version, Tokenizer tokenizer) {
    this.tokenizer = tokenizer;
    this.version = version;
  }

  public void startPage(long originalPageId, long indexPageId, CharSequence title, URI uri) {
    if (isProcessingPage) {
      throw new IllegalStateException("Duplicate startPage call: page is already being processed");
    }
    isProcessingPage = true;

    if (pageUri.containsKey(uri)) {
      throw new IllegalStateException("Uri " + uri + " is already in IndexMeta");
    }

    try {
      String uriDecoded = URLDecoder.decode(uri.toString(), "UTF-8");
      pageUri.put(uriDecoded, indexPageId);
    } catch (UnsupportedEncodingException e) {
      LOG.warn(e);
    }
    pageUri.put(uri.toString(), indexPageId);

    if (knownOriginalIds.contains(originalPageId)) {
      throw new IllegalStateException(
          String.format("Wiki id [ %d ] has been already received!", originalPageId));
    }
    knownOriginalIds.add(originalPageId);

    int titleSize = (int) tokenizer.parseTextToWords(title).count();
    wikiIdToTitleSize.put(originalPageId, titleSize);
  }

  public void addSection(Section section, long sectionId) {
    try {
      String uriDecoded = URLDecoder.decode(section.toString(), "UTF-8");
      pageUri.put(uriDecoded, sectionId);
    } catch (UnsupportedEncodingException e) {
      LOG.warn(e);
    }
    pageUri.put(section.toString(), sectionId);

    ++titlesCount;
    int sectionTitleTokensCount = (int) tokenizer.parseTextToWords(section.title()).count();

    int[] sectionTextTokensCount = new int[]{0};
    tokenizer.parseTextToWords(section.text())
        .map(CharSeqTools::toLowerCase)
        .mapToLong(t -> BrandNewIdGenerator.termIdGenerator(t).next())
        .forEach(t -> {
          termIds.add(t);
          ++sectionTextTokensCount[0];
        });

    totalTokenCount += sectionTextTokensCount[0] + sectionTitleTokensCount;
    titleTokensCount += sectionTitleTokensCount;

    for (Link link : section.links()) {
      incomingLinksCounts.adjustOrPutValue(link.targetId(), 1, 1);
    }
  }

  public void endPage() {
    if (!isProcessingPage) {
      throw new IllegalStateException("Illegal call to endPage: no page is being processed");
    }
    isProcessingPage = false;
  }

  public IndexMeta build() {
    List<UriPageMapping> mappings = new ArrayList<>();

    pageUri.forEachEntry((uri, id) -> {
      mappings.add(UriPageMapping.newBuilder().setPageId(id).setUri(uri).build());
      return true;
    });

    int[] targetTitleTokensCount = new int[]{0};
    int[] linksCount = new int[]{0};
    knownOriginalIds.forEach(id -> {
      int incomingLinksCount = incomingLinksCounts.containsKey(id) ? incomingLinksCounts.get(id) : 0;
      targetTitleTokensCount[0] += wikiIdToTitleSize.get(id) * incomingLinksCount;
      linksCount[0] += incomingLinksCount;
      return true;
    });

    return IndexMeta.newBuilder()
        .setVersion(version)
        .setLinksCount(linksCount[0])
        .setAverageTargetTitleSize((double) targetTitleTokensCount[0] / linksCount[0])
        .setTitlesCount(titlesCount)
        .setAverageTitleSize((double) titleTokensCount / titlesCount)
        .setAveragePageSize((double) totalTokenCount / knownOriginalIds.size())
        .setPagesCount(knownOriginalIds.size())
        .setVocabularySize(termIds.size())
        .addAllUriPageMappings(mappings)
        .build();
  }
}
