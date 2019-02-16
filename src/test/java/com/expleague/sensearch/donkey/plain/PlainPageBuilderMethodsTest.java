package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument.Link;
import com.expleague.sensearch.protobuf.index.IndexUnits;
import com.expleague.sensearch.protobuf.index.IndexUnits.Page;
import com.expleague.sensearch.utils.SensearchTestCase;
import com.google.common.collect.Lists;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class PlainPageBuilderMethodsTest extends SensearchTestCase {

  private final static URI DEFAULT_URI = URI.create("https://www.wikipedia.org/");

  static class SectionMock implements CrawlerDocument.Section {

    private String text = "";
    private final List<Link> links = new ArrayList<>();
    private List<CharSequence> title = new ArrayList<>();

    SectionMock() {

    }

    @Override
    public CharSequence title() {
      return "";
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
    public List<CharSequence> titles() {
      return title;
    }

    @Override
    public List<Link> links() {
      return links;
    }
    
    @Override
    public URI uri() {
      return DEFAULT_URI;
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
  public void resolveLinksTest() {
    List<IndexUnits.Page.Link.Builder> links = new ArrayList<>();
    links.add(Page.Link.newBuilder()
        .setSourcePageId(11)
        .setTargetPageId(2)
        .setText("l.11.12")
    );
    links.add(Page.Link.newBuilder()
        .setSourcePageId(11)
        .setTargetPageId(3)
        .setText("l.11.13")
    );
    links.add(Page.Link.newBuilder()
        .setSourcePageId(11)
        .setTargetPageId(4)
        .setText("l.11.14")
    );
    links.add(Page.Link.newBuilder()
        .setSourcePageId(12)
        .setTargetPageId(1)
        .setText("l.12.11")
    );
    links.add(Page.Link.newBuilder()
        .setSourcePageId(12)
        .setTargetPageId(4)
        .setText("l.12.14")
    );
    links.add(Page.Link.newBuilder()
        .setSourcePageId(12)
        .setTargetPageId(5)
        .setText("l.12.15")
    );
    links.add(Page.Link.newBuilder()
        .setSourcePageId(14)
        .setTargetPageId(1)
        .setText("l.14.11")
    );
    links.add(Page.Link.newBuilder()
        .setSourcePageId(14)
        .setTargetPageId(3)
        .setText("l.14.13")
    );

    TLongLongMap wikiToIndexMappings = new TLongLongHashMap();
    wikiToIndexMappings.put(1, 11);
    wikiToIndexMappings.put(2, 12);
    wikiToIndexMappings.put(3, 13);
    wikiToIndexMappings.put(4, 14);

    TLongObjectMap<List<IndexUnits.Page.Link>> incomingLinks = new TLongObjectHashMap<>();
    TLongObjectMap<List<IndexUnits.Page.Link>> outcomingLinks = new TLongObjectHashMap<>();
    PlainPageBuilder.resolveLinks(links, wikiToIndexMappings, outcomingLinks, incomingLinks);

    Assert.assertEquals(3, outcomingLinks.get(11).size());
    Assert.assertEquals(3, outcomingLinks.get(12).size());
    Assert.assertEquals(2, outcomingLinks.get(14).size());

    Assert.assertEquals(2, incomingLinks.get(14).size());
    Assert.assertEquals(2, incomingLinks.get(11).size());
  }
}
