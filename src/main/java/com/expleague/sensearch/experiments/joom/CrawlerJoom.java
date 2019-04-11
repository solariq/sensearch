package com.expleague.sensearch.experiments.joom;

import com.expleague.sensearch.core.Annotations.DataZipPath;
import com.expleague.sensearch.donkey.crawler.Crawler;
import com.expleague.sensearch.donkey.crawler.document.CrawlerDocument;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

public class CrawlerJoom implements Crawler {

  private final Path path;

  @Inject
  public CrawlerJoom(@DataZipPath Path zipPath) {
    this.path = zipPath;
  }

  @Override
  public Stream<CrawlerDocument> makeStream() throws IOException {
    int[] idsCnt = new int[]{0};

    return StreamSupport.stream(
        new AbstractSpliterator<CrawlerDocument>(
            Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.NONNULL) {
          private BufferedReader reader =
              new BufferedReader(
                  new InputStreamReader(new GZIPInputStream(Files.newInputStream(path))));
          private ObjectMapper mapper = new ObjectMapper();

          @Override
          public boolean tryAdvance(Consumer<? super CrawlerDocument> consumer) {

            try {
              String line = reader.readLine();
              if (line == null) {
                return false;
              }
              idsCnt[0] += 1;

              JoomRawDoc joomRawDoc = mapper.readValue(line, JoomRawDoc.class);
              if (joomRawDoc.origName == null) {
                joomRawDoc.origName = "";
              }
              if (joomRawDoc.origDescription == null) {
                joomRawDoc.origDescription = "";
              }
              if (joomRawDoc.id == null) {
                System.out.println("null id!");
                return true;
              }

              String id;
              if (joomRawDoc.id instanceof Map) {
                id = ((Map<String, String>) joomRawDoc.id).get("$oid");
              } else {
                id = (String) joomRawDoc.id;
              }
              JoomPage joomPage =
                  new JoomPage(joomRawDoc.origName, joomRawDoc.origDescription, id);
              consumer.accept(joomPage);
              return true;
            } catch (IOException e) {
              e.printStackTrace();
              // todo wtf
              return true;
            }
          }
        },
        false);
  }

  public static class JoomRawDoc {

    @JsonProperty("_id")
    Object id;

    @JsonProperty("origName")
    String origName;

    @JsonProperty("origDescription")
    String origDescription;

    @JsonProperty("origMainImageUrl")
    String origMainImageUrl;
  }
}
