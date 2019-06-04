package com.expleague.sensearch.donkey;

import com.expleague.commons.seq.CharSeq;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;

public interface PageTranslator {

  /**
   * Init local Crawler page to transform and remove previous Crawler page
   *
   * @param page - any {@link CrawlerDocument}
   */
  void addCrawlPage(CrawlerDocument page);

  /**
   * Init local Stream page to transform and remove previous Stream page
   *
   * @param page - any {@link StreamPage}
   */
  void addStreamPage(StreamPage page);

  /**
   * Init local Page to transform and remove previous Page
   *
   * @param page - any {@link Page}
   */
  void addPage(Page page);

  /**
   * Transform {@link CrawlerDocument} or {@link StreamPage} to {@link Page}
   *
   * @param type - type of stored Page
   * @return {@link Page}
   */
  Page createPage(PageStore type);

  /**
   * Transform {@link CrawlerDocument} or {@link Page} to {@link StreamPage}
   *
   * @param type - type of stored Page
   * @return {@link StreamPage}
   */
  StreamPage createStreamPage(PageStore type);

  /**
   * @return {@link TObjectIntMap} with tokens -> id
   */
  TObjectIntMap<CharSeq> termToIntMapping();


  /**
   * @return {@link TIntObjectMap} with id -> tokens
   */
  TIntObjectMap<CharSeq> intToTermMapping();

  enum PageStore {
    CRAWLER_PAGE,
    STREAM_PAGE,
    INDEX_PAGE
  }
}
