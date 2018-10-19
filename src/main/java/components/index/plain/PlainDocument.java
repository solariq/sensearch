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
    documentId = Long.parseLong(pathToIndexEntry.getFileName().toString());
    documentContentPath = pathToIndexEntry.resolve(PlainIndexBuilder.CONTENT_FILE);
    documentTitlePath = pathToIndexEntry.resolve(PlainIndexBuilder.META_FILE);
  }

  @Override
  public long getId() {
    return documentId;
  }

  @Override
  public CharSequence getContent() throws IOException {
    StringBuilder contentBuilder = new StringBuilder();
    try(BufferedReader bufferedReader = Files.newBufferedReader(documentContentPath)) {
      bufferedReader.lines().forEach(contentBuilder::append);
    }
    //Why toString()?
    return contentBuilder.toString();
  }

  @Override
  public CharSequence getTitle() throws IOException{
    StringBuilder titleBuilder = new StringBuilder();
    try(BufferedReader bufferedReader = Files.newBufferedReader(documentTitlePath)) {
      bufferedReader.lines().forEach(titleBuilder::append);
    }

    return titleBuilder.toString();
  }
}
