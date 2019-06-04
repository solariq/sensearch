package com.expleague.sensearch.donkey;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;

public interface PageTranslator {

  /**
   * Init local Crawler page to transform
   *
   * @param page - any {@link CrawlerDocument}
   */
  void addCrawlPage(CrawlerDocument page);

  /**
   * Init local Stream page to transform
   *
   * @param page - any {@link StreamPage}
   */
  void addStreamPage(StreamPage page);

  /**
   * Transform {@link CrawlerDocument} or {@link StreamPage} to
   * {@link Page}
   *
   * @param type - type of stored Page
   *
   * @return {@link Page}
   */
  Page createPage(PageStore type);

  /**
   * Transform {@link CrawlerDocument} or {@link Page} to
   * {@link StreamPage}
   *
   * @param type - type of stored Page
   *
   * @return {@link StreamPage}
   */
  StreamPage createStreamPage(PageStore type);

  enum PageStore {
    CRAWLER_PAGE,
    STREAM_PAGE,
    INDEX_PAGE
  }
}
