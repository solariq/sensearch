package components.crawler;

import components.crawler.document.CrawlerDocument;

public interface Crawler {

    long getID(CrawlerDocument crawlerDocument); //???

    CrawlerDocument getDocument(long iD);
}
