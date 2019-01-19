package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta.UriPageMapping;
import gnu.trove.map.TObjectLongMap;
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
  private final TObjectLongMap<String> pageUri = new TObjectLongHashMap<>();
  private int totalTokenCount = 0;
  private boolean isProcessingPage = false;

  public IndexMetaBuilder(int version) {
    this.version = version;
  }

  public void startPage(long pageId, URI uri) {
    if (isProcessingPage) {
      throw new IllegalStateException("Duplicate startPage call: page is already being processed");
    }
    isProcessingPage = true;

    if (pageUri.containsKey(pageUri)) {
      throw new IllegalStateException("Uri " + uri + " is already in IndexMeta");
    }
    String uriDecoded = null;
    try {
      uriDecoded = URLDecoder.decode(uri.toString(), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      LOG.warn(e);
    }
    pageUri.put(uriDecoded, pageId);
  }

  public void acceptTermId(long termId) {
    totalTokenCount += 1;
    termIds.add(termId);
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

    return IndexMeta.newBuilder()
        .setVersion(version)
        .setAveragePageSize(1.0 * pageUri.size() / totalTokenCount)
        .setPagesCount(pageUri.size())
        .setVocabularySize(termIds.size())
        .addAllUriPageMappings(mappings)
        .build();
  }

  public void addSection(URI sectionUri, long sectionId) {
    pageUri.put(sectionUri.toString(), sectionId);
  }
}
