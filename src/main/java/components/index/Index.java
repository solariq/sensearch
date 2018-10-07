package components.index;

import components.crawler.document.CrawlerDocument;

/**
 * Created by sandulmv on 06.10.18.
 */
public interface Index {
  CrawlerDocument getDocument(long documentId);
  long[] getDocumentIds();
  int size();
}
