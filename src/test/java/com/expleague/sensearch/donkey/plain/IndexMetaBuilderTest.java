package com.expleague.sensearch.donkey.plain;

import static org.junit.Assert.assertEquals;

import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta.UriPageMapping;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;

public class IndexMetaBuilderTest {

  @Test
  public void test() {
    IndexMetaBuilder metaBuilder = new IndexMetaBuilder(1);
    metaBuilder.startPage(1, URI.create("uri1"));
    metaBuilder.acceptTermId(-1);
    metaBuilder.acceptTermId(-2);
    metaBuilder.acceptTermId(-1);
    metaBuilder.endPage();

    metaBuilder.startPage(2, URI.create("uri2"));
    metaBuilder.acceptTermId(-2);
    metaBuilder.acceptTermId(-3);
    metaBuilder.acceptTermId(-9);
    metaBuilder.endPage();

    metaBuilder.startPage(3, URI.create("uri3"));
    metaBuilder.acceptTermId(-239);
    metaBuilder.endPage();

    IndexMeta meta = metaBuilder.build();
    assertEquals(1, meta.getVersion());
    assertEquals(3.0 / (3 + 3 + 1), meta.getAveragePageSize(), 1e-8);
    assertEquals(3, meta.getPagesCount());
    assertEquals(5, meta.getVocabularySize());

    Map<String, Long> uriMap =
        meta.getUriPageMappingsList()
            .stream()
            .collect(Collectors.toMap(UriPageMapping::getUri, UriPageMapping::getPageId));
    assertEquals(1, uriMap.get("uri1").longValue());
    assertEquals(2, uriMap.get("uri2").longValue());
    assertEquals(3, uriMap.get("uri3").longValue());
    assertEquals(3, uriMap.size());
  }
}
