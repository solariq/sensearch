package components.searcher;

import com.expleague.commons.util.Pair;
import components.crawler.document.CrawlerDocument;

/**
 * Created by sandulmv on 06.10.18.
 */
public abstract class Index implements Iterable<Pair<Long, CrawlerDocument>> {
  protected String pathToIndex;
  public Index(String pathToIndex) {
    this.pathToIndex = pathToIndex;
  }

  public abstract CrawlerDocument getDocument(long documentId);
}
