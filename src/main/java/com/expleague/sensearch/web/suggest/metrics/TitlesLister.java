package com.expleague.sensearch.web.suggest.metrics;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import org.apache.commons.compress.archivers.dump.DumpArchiveConstants.SEGMENT_TYPE;
import com.expleague.sensearch.AppModule;
import com.expleague.sensearch.Config;
import com.expleague.sensearch.ConfigImpl;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class TitlesLister {
  public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
    Config config =
        new ObjectMapper().readValue(Paths.get("./config.json").toFile(), ConfigImpl.class);
    Injector injector = Guice.createInjector(new AppModule(config));

    Index index = injector.getInstance(Index.class);
    
    try (PrintWriter out = new PrintWriter(new FileOutputStream("sugg_dataset/titles"))) {
      index.allDocuments()
      .forEach(d -> {
        out.println(
            index.parse(d.content(Page.SegmentType.SUB_BODY))
            .skip(50)
            .limit(10)
            .map(Term::text)
            .collect(Collectors.joining(" "))
            );
      });
    }
  }
}
