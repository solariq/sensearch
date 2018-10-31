package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.index.IndexedPage;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 */
public class PlainDocument implements IndexedPage {

  private final long documentId;
  private final Path documentContentPath;
  private final Path documentTitlePath;

  PlainDocument(Path pathToIndexEntry) {
    this.documentId = Long.parseLong(pathToIndexEntry.getFileName().toString());
    this.documentContentPath = pathToIndexEntry.resolve(PlainIndexBuilder.CONTENT_FILE);
    this.documentTitlePath = pathToIndexEntry.resolve(PlainIndexBuilder.META_FILE);
  }

  @Override
  public long id() {
    return this.documentId;
  }

  @Override
  public URI reference() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CharSequence text() {
    StringBuilder contentBuilder = new StringBuilder();
    try (BufferedReader bufferedReader = Files.newBufferedReader(documentContentPath)) {
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
    try (BufferedReader bufferedReader = Files.newBufferedReader(documentTitlePath)) {
      bufferedReader.lines().forEach(titleBuilder::append);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Can not get title for the document with id %d", this.id()), e
      );
    }

    return titleBuilder;
  }
}
