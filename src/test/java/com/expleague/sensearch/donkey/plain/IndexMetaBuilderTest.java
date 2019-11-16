package com.expleague.sensearch.donkey.plain;

import static org.junit.Assert.assertEquals;

import com.expleague.sensearch.donkey.plain.IndexMetaBuilder.TermSegment;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta;
import org.junit.Test;

public class IndexMetaBuilderTest {

  // TODO FIXME
  // TODO test addSection
  @Test
  public void test() {
    IndexMetaBuilder metaBuilder = new IndexMetaBuilder(1);
    metaBuilder.startPage(1, 1); //Title
    metaBuilder.addTerm(-1, TermSegment.TEXT);
    metaBuilder.addTerm(-1, TermSegment.SECTION_TITLE);
    metaBuilder.addTerm(-1, TermSegment.SECTION_TITLE);
    metaBuilder.addTerm(-2, TermSegment.TEXT);
    metaBuilder.addTerm(-1, TermSegment.TEXT);
    metaBuilder.addSection(-5);
    metaBuilder.addSection(-5);
    metaBuilder.endPage();

    metaBuilder.startPage(2, 2); //Title title
    metaBuilder.addTerm(-2, TermSegment.TEXT);
    metaBuilder.addTerm(-3, TermSegment.TEXT);
    metaBuilder.addTerm(-1, TermSegment.SECTION_TITLE);
    metaBuilder.addTerm(-9, TermSegment.TEXT);
    metaBuilder.addSection(-10);
    metaBuilder.endPage();

    metaBuilder.startPage(3, 3); //Title with title
    metaBuilder.addTerm(-239, TermSegment.TEXT);
    metaBuilder.addSection(-13);
    metaBuilder.endPage();

    IndexMeta meta = metaBuilder.build();
    assertEquals(1, meta.getVersion());
    assertEquals(3, meta.getDocumentsCount());
    assertEquals(7, meta.getContentTermsCount());
    assertEquals(4, meta.getTitlesCount());
    assertEquals(3, meta.getTitleTermsCount());
    assertEquals(0, meta.getLinksCount());
    assertEquals(0, meta.getTargetTitleTermsCount());
    assertEquals(0, meta.getLinkTermsCount());
  }
}
