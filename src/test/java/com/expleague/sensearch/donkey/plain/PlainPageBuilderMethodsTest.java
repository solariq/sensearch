package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Section;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.utils.SensearchTestCase;
import com.google.common.collect.Lists;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class PlainPageBuilderMethodsTest extends SensearchTestCase {

  private static class SectionMock implements CrawlerDocument.Section {

    private String text = "";
    private List<Link> links = new ArrayList<>();
    private List<CharSequence> title = new ArrayList<>();

    SectionMock() {

    }

    static class LinkMock implements CrawlerDocument.Link {

      private String text = "";
      private long targetId = 0;

      LinkMock() {

      }
      @Override
      public CharSequence text() {
        return text;
      }

      @Override
      public CharSequence targetTitle() {
        return "";
      }

      @Override
      public long targetId() {
        return targetId;
      }

      @Override
      public int textOffset() {
        return 0;
      }

      LinkMock text(String text) {
        this.text = text;
        return this;
      }

      LinkMock targetId(long id) {
        this.targetId = id;
        return this;
      }
    }

    @Override
    public CharSequence text() {
      return text;
    }

    @Override
    public List<CharSequence> title() {
      return title;
    }

    @Override
    public List<Link> links() {
      return links;
    }
    
    @Override
    public URI uri() {
      return null;
    }

    SectionMock addLink(Link link) {
      this.links.add(link);
      return this;
    }

    SectionMock text(String text) {
      this.text = text;
      return this;
    }

    SectionMock title(String ... title) {
      this.title = Lists.newArrayList(title);
      return this;
    }
  }


  @Test
  public void resolveLinkTest() {

  }

  @Test
  public void toPagesTreeStructureTest() {
    List<Section> sections = new ArrayList<>();
    sections.add(new SectionMock().text("text1").title("s1"));
    sections.add(new SectionMock().text("text1").title("s1", "s1.1"));
    sections.add(new SectionMock().text("text1").title("s1", "s1.1", "s1.1.1"));
    sections.add(new SectionMock().text("text1").title("s1", "s1.1", "s1.1.2"));
    sections.add(new SectionMock().text("text1").title("s1", "s1.1", "s1.1.2", "s1.1.2.1"));
    sections.add(new SectionMock().text("text1").title("s1", "s1.1", "s1.1.2", "s1.1.2.2"));
    sections.add(new SectionMock().text("text1").title("s1", "s1.1", "s1.1.3"));
    sections.add(new SectionMock().text("text1").title("s1", "s1.1", "s1.1.3", "s1.1.3.1"));
    sections.add(new SectionMock().text("text1").title("s1", "s1.2"));
    sections.add(new SectionMock().text("text1").title("s1", "s1.3"));

    long rootId = - (1L << 5);
    List<Page> pages = PlainPageBuilder.toPages(sections, rootId, new ArrayList<>());

    Assert.assertEquals(10, pages.size());

    TLongObjectMap<Page> pagesMap = new TLongObjectHashMap<>();
    for (Page p : pages) {
      pagesMap.put(p.getPageId(), p);
    }

    pagesMap.forEachEntry(
        (id, p) -> {
          if ("s1".equals(p.getTitle())) {
            Assert.assertFalse(p.hasParentId());
            Assert.assertEquals(rootId, p.getPageId());
            Assert.assertEquals(3, p.getSubpagesIdsCount());

            Set<String> subpagesTitles = new HashSet<>();
            subpagesTitles.add("s1.1");
            subpagesTitles.add("s1.2");
            subpagesTitles.add("s1.3");

            Set<String> actualTitles = new HashSet<>();
            for (long sid : p.getSubpagesIdsList()) {
              actualTitles.add(pagesMap.get(sid).getTitle());
            }

            Assert.assertEquals(subpagesTitles, actualTitles);
          }

          if ("s1.1".equals(p.getTitle())) {
            Assert.assertTrue(p.hasParentId());
            Assert.assertEquals(rootId, p.getParentId());
            Assert.assertEquals(3, p.getSubpagesIdsCount());

            Set<String> subpagesTitles = new HashSet<>();
            subpagesTitles.add("s1.1.1");
            subpagesTitles.add("s1.1.2");
            subpagesTitles.add("s1.1.3");

            Set<String> actualTitles = new HashSet<>();
            for (long sid : p.getSubpagesIdsList()) {
              actualTitles.add(pagesMap.get(sid).getTitle());
            }

            Assert.assertEquals(subpagesTitles, actualTitles);
          }

          if ("s1.2".equals(p.getTitle())) {
            Assert.assertTrue(p.hasParentId());
            Assert.assertEquals(rootId, p.getParentId());
            Assert.assertEquals("s1", pagesMap.get(rootId).getTitle());
            Assert.assertEquals(0, p.getSubpagesIdsCount());
          }

          if ("s1.3".equals(p.getTitle())) {
            Assert.assertTrue(p.hasParentId());
            Assert.assertEquals(rootId, p.getParentId());
            Assert.assertEquals("s1", pagesMap.get(rootId).getTitle());
            Assert.assertEquals(0, p.getSubpagesIdsCount());
          }

          if ("s1.1.1".equals(p.getTitle())) {
            Assert.assertTrue(p.hasParentId());
            Assert.assertEquals("s1.1", pagesMap.get(p.getParentId()).getTitle());
            Assert.assertEquals(0, p.getSubpagesIdsCount());
          }

          if ("s1.1.2".equals(p.getTitle())) {
            Assert.assertTrue(p.hasParentId());
            Assert.assertEquals("s1.1", pagesMap.get(p.getParentId()).getTitle());
            Assert.assertEquals(2, p.getSubpagesIdsCount());

            Set<String> subpagesTitles = new HashSet<>();
            subpagesTitles.add("s1.1.2.1");
            subpagesTitles.add("s1.1.2.2");

            Set<String> actualTitles = new HashSet<>();
            for (long sid : p.getSubpagesIdsList()) {
              actualTitles.add(pagesMap.get(sid).getTitle());
            }

            Assert.assertEquals(subpagesTitles, actualTitles);
          }

          return true;
        }
    );
  }

  @Test
  @Ignore
  // TODO: write test
  public void resolveLinksTest() {

  }
}
