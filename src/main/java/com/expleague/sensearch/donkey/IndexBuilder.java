package com.expleague.sensearch.donkey;

import java.io.IOException;
import java.nio.file.Path;

public interface IndexBuilder {

  void buildIndex() throws IOException;

  void buildIndex(Path embeddingPath) throws IOException;
}
