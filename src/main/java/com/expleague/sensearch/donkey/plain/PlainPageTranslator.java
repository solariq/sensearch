package com.expleague.sensearch.donkey.plain;

import com.expleague.commons.seq.CharSeq;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.Page.SegmentType;
import com.expleague.sensearch.donkey.PageTranslator;
import com.expleague.sensearch.donkey.StreamPage;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.expleague.sensearch.donkey.utils.PageParser;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class PlainPageTranslator implements PageTranslator {

  private CrawlerDocument crawlPage = null;
  private StreamPage streamPage = null;
  private Page page = null;

  private TObjectIntMap<CharSeq> termToIntMap = new TObjectIntHashMap<>();
  private TIntObjectMap<CharSeq> intToTermMap = new TIntObjectHashMap<>();
  private final PageParser parser = new PageParser();

  private static int ID = 0;
  public final static CharSeq END_OF_PARAGRAPH = CharSeq.create("777@END@777");
  private final int END_ID;
  private final static CharSeq END_OF_TITLE = CharSeq.create(Page.TITLE_DELIMETER);
  private final int END_TITLE;
  public final static int FIRST_UPPERCASE = 0x00000080;
  public final static int ALL_UPPERCASE = 0x00000040;

  public PlainPageTranslator() {
    END_ID = addToken(END_OF_PARAGRAPH);
    END_TITLE = addToken(END_OF_TITLE);
  }

  @Override
  public void addCrawlPage(CrawlerDocument page) {
    this.crawlPage = page;
  }

  @Override
  public void addStreamPage(StreamPage page) {
    this.streamPage = page;
  }

  @Override
  public void addPage(Page page) {
    this.page = page;
  }

  @Override
  public Page createPage(PageStore type) {
    Page page = null;
    switch (type) {
      case INDEX_PAGE:
        return this.page;
      case STREAM_PAGE:
        if (this.streamPage == null) {
          return null;
        } else {
          page = StreamToPage();
        }
        break;
      case CRAWLER_PAGE:
        if (this.crawlPage == null) {
          return null;
        } else {
          page = CrawlToPage();
        }
        break;
    }
    return page;
  }

  @Override
  public StreamPage createStreamPage(PageStore type) {
    StreamPage page = null;
    switch (type) {
      case INDEX_PAGE:
        if (this.page == null) {
          return null;
        } else {
          page = PageToStream();
        }
        break;
      case STREAM_PAGE:
        return this.streamPage;
      case CRAWLER_PAGE:
        if (this.crawlPage == null) {
          return null;
        } else {
          page = CrawlToStream();
        }
        break;
    }
    return page;
  }

  @Override
  public TObjectIntMap<CharSeq> termToIntMapping() {
    return termToIntMap;
  }

  @Override
  public TIntObjectMap<CharSeq> intToTermMapping() {
    return intToTermMap;
  }

  private StreamPage CrawlToStream() {
    TIntList res = new TIntArrayList();

    parser.parse(CharSeq.create(crawlPage.title()))
        .forEach(token -> {
          int id = addToken(token);
          res.add(id);
        });
    res.add(END_TITLE);
    res.add(END_ID);
    res.add(END_ID); //no text in root section

    crawlPage.sections()
        .forEach(s -> {
          s.titles().forEach(title -> {
            parser.parse(CharSeq.create(title))
                .forEach(token -> {
                  int id = addToken(token);
                  res.add(id);
                });
            res.add(END_TITLE);
          });
          res.add(END_ID);
          parser.parse(CharSeq.create(s.text()))
              .forEach(token -> {
                int id = addToken(token);
                res.add(id);
              });
          res.add(END_ID);
        });

    return null;
  }

  private TIntList parseFullTitle(CharSequence titles) {
    TIntList res = new TIntArrayList();
    for (String title : titles.toString().split(" # ")) {
      parser.parse(CharSeq.create(title)).forEach(token -> {
        int id = addToken(token);
        res.add(id);
      });
      res.add(END_TITLE);
    }
    return res;
  }

  private StreamPage PageToStream() {
    TIntList res = new TIntArrayList();

    res.addAll(parseFullTitle(page.content(SegmentType.FULL_TITLE)));
    res.add(END_ID);
    parser.parse(CharSeq.create(page.content(SegmentType.SUB_BODY)))
        .forEach(token -> {
          int id = addToken(token);
          res.add(id);
        });
    res.add(END_ID);
    page.subpages().forEach(subPage -> {
      res.addAll(parseFullTitle(subPage.content(SegmentType.FULL_TITLE)));
      res.add(END_ID);
      parser.parse(CharSeq.create(subPage.content(SegmentType.SUB_BODY)))
          .forEach(token -> {
            int id = addToken(token);
            res.add(id);
          });
      res.add(END_ID);
    });

    return null;
  }

  private Page StreamToPage() {
    return null;
  }

  private Page CrawlToPage() {
    return null;
  }

  private int addToken(CharSeq token) {
    boolean firstUp = false;
    final boolean[] allUp = {true};
    int id;
    if (Character.isUpperCase(token.at(0))) {
      firstUp = true;
    }
    token.forEach(c -> {
      if (Character.isLowerCase(c)) {
        allUp[0] = false;
      }
    });
    CharSeq lowToken = CharSeq.create(token.toString().toLowerCase());
    if (termToIntMap.containsKey(lowToken)) {
      id = termToIntMap.get(lowToken);
    } else {
      id = ID;
      termToIntMap.put(lowToken, id);
      intToTermMap.put(id, lowToken);
      ID++;
    }
    id = id << 8;
    if (firstUp) {
      id |= FIRST_UPPERCASE;
    }
    if (allUp[0]) {
      id |= ALL_UPPERCASE;
    }
    return id;
  }
}
