package components.searcher;

import components.crawler.document.CrawlerDocument;
import components.queryTmp.Query;

/**
 * Created by sandulmv on 07.10.18.
 */
@FunctionalInterface
public interface FeatureExtractor {
  Features extractFeatures(Query query, CrawlerDocument document);
}
