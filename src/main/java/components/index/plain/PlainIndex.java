package components.index.plain;

import components.index.Index;
import components.index.IndexedDocument;
import components.query.Query;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class PlainIndex implements Index {

  private static final Pattern INDEX_ENTRY_NAME_PATTERN = Pattern.compile("\\d+");

  private final Path indexRoot;
  private final TLongSet availableDocuments;

  PlainIndex(Path indexRoot) throws IOException {
    this.indexRoot = indexRoot;

    availableDocuments = new TLongHashSet();
    Files.list(indexRoot)
        .filter(entry -> INDEX_ENTRY_NAME_PATTERN.matcher(
            entry.getFileName().toString()).matches()
        )
        .mapToLong(entry -> Long.parseLong(entry.getFileName().toString()))
        .forEach(availableDocuments::add);
  }

  @Override
  public Stream<IndexedDocument> fetchDocuments(Query query) {
    return LongStream.of(availableDocuments.toArray())
        .mapToObj(id -> indexRoot.resolve(Long.toString(id)))
        .map(PlainDocument::new);
  }
}
