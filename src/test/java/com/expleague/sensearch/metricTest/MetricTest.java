package com.expleague.sensearch.metricTest;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.SenSeArch;
import com.expleague.sensearch.web.Builder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import org.junit.Before;
import org.junit.Test;

public class MetricTest {

  private Builder builder;
  private Config config;
  private SenSeArch searcher;

  @Before
  public void initSearch() throws IOException, XMLStreamException {
    Injector injector = Guice.createInjector(new AppModule());
    builder = injector.getInstance(Builder.class);
    config = builder.build(new LocalRequestCrawler());
    searcher = builder.getSearcher();
  }

  @Test
  public void metricTest() {
    searcher.search("test", 0);
  }
}
