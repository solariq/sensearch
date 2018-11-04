package com.expleague.sensearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;

import java.io.IOException;
import java.nio.file.Paths;

public class AppModule extends AbstractModule {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  protected void configure() {
    try {
      Config config = objectMapper.readValue(Paths.get("./config.json").toFile(), Config.class);
      bind(Config.class).toInstance(config);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
