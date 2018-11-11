package com.expleague.sensearch.donkey;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.donkey.crawler.Crawler;
import java.io.IOException;

/**
 * Created by sandulmv on 11.11.18.
 */
public interface IndexBuilder {
  void buildIndex(Crawler crawler, Config config) throws IOException;
}
