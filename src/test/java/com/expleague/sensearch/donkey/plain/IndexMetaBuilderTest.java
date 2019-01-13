package com.expleague.sensearch.donkey.plain;

import static org.junit.Assert.assertEquals;

import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta.UriPageMapping;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;

public class IndexMetaBuilderTest {

  @Test
  public void test() {
    IndexMetaBuilder metaBuilder = new IndexMetaBuilder(1);
    metaBuilder.acceptPage(1, 10, "uri1");
    metaBuilder.acceptTermId(-1);
    metaBuilder.acceptTermId(-2);
    metaBuilder.acceptTermId(-1);
    metaBuilder.acceptPage(2, 15, "uri2");
    metaBuilder.acceptTermId(-2);
    metaBuilder.acceptTermId(-3);
    metaBuilder.acceptTermId(-9);
    metaBuilder.acceptPage(3, 2, null);
    metaBuilder.acceptTermId(-239);
    metaBuilder.acceptPage(4, 1, null);
    metaBuilder.acceptTermId(-1);
    metaBuilder.acceptPage(5, 2, "uri5");
    metaBuilder.acceptTermId(-1);
    metaBuilder.acceptTermId(-1);
    metaBuilder.acceptTermId(-10);

    IndexMeta meta = metaBuilder.build();
    assertEquals(1, meta.getVersion());
    assertEquals(5.0 / (10 + 15 + 2 + 1 + 2), meta.getAveragePageSize(), 1e-8);
    assertEquals(5, meta.getPagesCount());
    assertEquals(6, meta.getVocabularySize());

    Map<String, Long> uriMap =
        meta.getUriPageMappingsList()
            .stream()
            .collect(Collectors.toMap(UriPageMapping::getUri, UriPageMapping::getPageId));
    assertEquals(1, uriMap.get("uri1").longValue());
    assertEquals(2, uriMap.get("uri2").longValue());
    assertEquals(5, uriMap.get("uri5").longValue());
    assertEquals(3, uriMap.size());
  }
}
