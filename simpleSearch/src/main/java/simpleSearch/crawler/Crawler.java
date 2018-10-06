package simpleSearch.crawler;

import simpleSearch.crawler.document.CrawlerDocument;

public interface Crawler {

    long getID(CrawlerDocument crawlerDocument);

    CrawlerDocument getDocument(long iD);
}
