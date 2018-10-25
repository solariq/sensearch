package components.index.plain;

import components.index.IndexedDocument;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 */
public class PlainDocument implements IndexedDocument {

  private final long documentId;
  private final Path documentContentPath;
  private final Path documentTitlePath;

  PlainDocument(Path pathToIndexEntry) {
    this.documentId = Long.parseLong(pathToIndexEntry.getFileName().toString());
    this.documentContentPath = pathToIndexEntry.resolve(PlainIndexBuilder.CONTENT_FILE);
    this.documentTitlePath = pathToIndexEntry.resolve(PlainIndexBuilder.META_FILE);
  }

  @Override
  public long getId() {
    return this.documentId;
  }

  @Override
  public CharSequence getContent() {
    StringBuilder contentBuilder = new StringBuilder();
    try (BufferedReader bufferedReader = Files.newBufferedReader(documentContentPath)) {
      bufferedReader.lines().forEach(contentBuilder::append);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Can not get content for the document with id %d", this.getId()), e
      );
    }

    return contentBuilder;
  }

  @Override
  public CharSequence getTitle() {
    StringBuilder titleBuilder = new StringBuilder();
    try (BufferedReader bufferedReader = Files.newBufferedReader(documentTitlePath)) {
      bufferedReader.lines().forEach(titleBuilder::append);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Can not get title for the document with id %d", this.getId()), e
      );
    }

    return titleBuilder;
  }
}
