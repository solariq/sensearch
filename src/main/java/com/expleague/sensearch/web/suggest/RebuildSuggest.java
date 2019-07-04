package com.expleague.sensearch.web.suggest;

import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.donkey.IndexCreator;
import com.expleague.sensearch.donkey.plain.PlainIndexCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;

public class RebuildSuggest {
  public static void main(String[] args) throws IOException {
    
    Properties logProperties = new Properties();
    logProperties.load(Files.newInputStream(Paths.get("log4j.properties")));
    PropertyConfigurator.configure(logProperties);
    
    Config config =
        new ObjectMapper().readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);
    Injector injector = Guice.createInjector(new AppModule(config));

    IndexCreator indexCreator = injector.getInstance(IndexCreator.class);

    ((PlainIndexCreator) indexCreator).createSuggest();
  }
}
