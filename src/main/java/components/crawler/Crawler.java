package components.crawler;

import components.crawler.document.CrawlerDocument;

public interface Crawler {

    //TODO return Stream

    CrawlerDocument getDocument(long iD);
}
