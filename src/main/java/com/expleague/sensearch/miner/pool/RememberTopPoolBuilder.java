package com.expleague.sensearch.miner.pool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public abstract class RememberTopPoolBuilder {

  private final ObjectMapper mapper = new ObjectMapper();

  public abstract Path getRememberDir();

  private File getRememberTopFile(String queryString) {
    return getRememberDir().resolve("query_" + queryString).toFile();
  }

  List<URI> getSavedQueryTop(String queryString) {
    try {
      return mapper.readValue(getRememberTopFile(queryString), new TypeReference<List<URI>>() {
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  void saveQueryTop(String queryString, List<URI> l) {
    try {
      Files.createDirectories(getRememberDir());
      mapper.writeValue(getRememberTopFile(queryString), l);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
