package com.expleague.sensearch.donkey;

import java.io.IOException;

public interface IndexBuilder {

  void buildIndexAndEmbedding() throws IOException;

  void buildIndex() throws IOException;

  void buildPageAndTermIndices();
  void buildLinks();
  void buildStats();
  void buildEmbedding();
}
