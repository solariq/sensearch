package simpleSearch.crawler;

import simpleSearch.crawler.document.Document;

import java.util.List;

public interface Crawler {

    long getID(Document document);

    Document getDocument(long iD);

}
