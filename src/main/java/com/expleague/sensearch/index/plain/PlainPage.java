package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.index.IndexedPage;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class PlainPage implements IndexedPage {
  private static final JsonParser JSON_PARSER = new JsonParser();
  private static final String ID_FIELD = "id";
  private static final String TITLE_FIELD = "title";

  private int id;

  PlainPage(String plainPageJson) {

  }

  @Override
  public long id() {
    return this.id;
  }

  @Override
  public URI reference() {
    return URI.create("http://ru.wikipedia.org/wiki/"
        + title().toString().replace(" ", "_"));
  }

  @Override
  public CharSequence text() {
    StringBuilder contentBuilder = new StringBuilder();
    try (BufferedReader bufferedReader = Files.newBufferedReader(contentPath)) {
      bufferedReader.lines().forEach(contentBuilder::append);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Can not get content for the document with id %d", this.id()), e
      );
    }

    return contentBuilder;
  }

  @Override
  public CharSequence title() {
    StringBuilder titleBuilder = new StringBuilder();
    try (BufferedReader bufferedReader = Files.newBufferedReader(titlePath)) {
      bufferedReader.lines().forEach(titleBuilder::append);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Can not get title for the document with id %d", this.id()), e
      );
    }

    return titleBuilder;
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof PlainPage) &&
        (this == other || ((PlainPage) other).id == this.id);
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.id);
  }
}
