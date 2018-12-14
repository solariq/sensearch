package com.expleague.sensearch.web;

import com.expleague.sensearch.Config;
import com.google.inject.Singleton;
import java.io.IOException;
import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;

@Singleton
public class Builder {

  private Config config;

  @Inject
  public Builder(Config config) {
    this.config = config;
  }

  public Config build() throws IOException, XMLStreamException {
    return config;
  }

  public int pageSize() {
    return 10;
  }

  public int windowSize() {
    return 4;
  }
}
