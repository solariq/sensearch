package components.index.plain;

import components.crawler.document.CrawlerDocument;
import components.index.Index;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;

/**
 * Created by sandulmv on 17.10.18.
 * Should be replaced with interface or abstract class?
 * Straightforward index builder that saves all documents received from Crawler
 * in separate text documents. Saves only title and content of each document
 */
public class PlainIndexBuilder {

  static final String CONTENT_FILE = "content";
  static final String META_FILE = "meta";

  private static final Logger LOG = Logger.getLogger(PlainIndexBuilder.class.getName());

  private Path indexRoot;

  public PlainIndexBuilder(Path indexRoot) {
    this.indexRoot = indexRoot;
  }

  private static void flushNewIndexEntry(Path indexRoot, CrawlerDocument parsedDocument) {
    Path newIndexEntryPath = indexRoot.resolve(Long.toString(System.currentTimeMillis()));
    if (Files.exists(newIndexEntryPath)) {
      LOG.warning(
          String.format(
              "Entry already exists: %s. Will delete old entry...",
              newIndexEntryPath.toAbsolutePath().toString()
          )
      );
      try {
        FileUtils.deleteDirectory(newIndexEntryPath.toFile());
      } catch (IOException e) {
        throw new RuntimeException("Failed to create new index entry!", e);
      }
    }
    try (
        BufferedWriter contentWriter = new BufferedWriter(
            new OutputStreamWriter(
                Files.newOutputStream(newIndexEntryPath.resolve(CONTENT_FILE))
            )
        );

        BufferedWriter titleWriter = new BufferedWriter(
            new OutputStreamWriter(
                Files.newOutputStream(newIndexEntryPath.resolve(META_FILE))
            )
        )
    ) {
      contentWriter.write(parsedDocument.returnContent().toString());
      titleWriter.write(parsedDocument.getTitle());
    } catch (Exception e) {
      LOG.warning
          (String.format("Failed to flush new index entry! Cause: %s",
              e.toString())
          );
      try {
        Files.deleteIfExists(newIndexEntryPath);
      } catch (IOException ioe) {
        LOG.severe(String.format("Failed to remove directory: %s. Cause: %s",
            newIndexEntryPath.toAbsolutePath(),
            ioe.toString())
        );
      }
    }
  }

  public Index buildIndex(Stream<CrawlerDocument> parsedDocumentsStream) throws IOException {
    parsedDocumentsStream.forEach(
        doc -> flushNewIndexEntry(this.indexRoot, doc)
    );
    try {
      return new PlainIndex(indexRoot);
    } catch (IOException e) {
      try {
        FileUtils.deleteDirectory(indexRoot.toFile());
      } catch (IOException ignore) {
        LOG.severe(
            String.format(
                "Index failure: failed to cleanup index root: %s. Cause: %s",
                indexRoot,
                ignore.toString()
            )
        );
      }
      throw new IOException("Failed to create index!", e);
    }
  }

}
