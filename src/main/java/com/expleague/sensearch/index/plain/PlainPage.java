package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.index.IndexedPage;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class PlainPage implements IndexedPage {

  private final long id;
  private final Path contentPath;
  private final Path titlePath;

  PlainPage(Path pathToIndexEntry) {
    this.id = Long.parseLong(pathToIndexEntry.getFileName().toString());
    this.contentPath = pathToIndexEntry.resolve(PlainIndexBuilder.CONTENT_FILE);
    this.titlePath = pathToIndexEntry.resolve(PlainIndexBuilder.META_FILE);
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
