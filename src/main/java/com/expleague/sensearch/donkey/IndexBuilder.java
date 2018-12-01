package com.expleague.sensearch.donkey;

import com.expleague.sensearch.Config;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.donkey.crawler.Crawler;
import java.io.IOException;

public interface IndexBuilder {

  void buildIndex(Crawler crawler, Config config, Lemmer lemmer) throws IOException;
}
