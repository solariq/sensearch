package simpleSearch.crawler;

import simpleSearch.crawler.document.MyDocument;

public interface Crawler {

    long getID(MyDocument myDocument);

    MyDocument getDocument(long iD);

}
