package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Filter;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.statistics.Stats;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.query.term.Term;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class PlainIndex implements Index {

  private static final Pattern INDEX_ENTRY_NAME_PATTERN = Pattern.compile("\\d+");

  private final Path indexRoot;
  private final TLongSet availableDocuments;

  private final Filter filter;
  
  private Stats statistics = new Stats();
  
  PlainIndex(Path indexRoot, Filter filter) throws IOException {
    this.indexRoot = indexRoot;
    this.filter = filter;

    availableDocuments = new TLongHashSet();
    Files.list(indexRoot)
        .filter(entry -> INDEX_ENTRY_NAME_PATTERN.matcher(
            entry.getFileName().toString()).matches()
        )
        .peek(p -> statistics.acceptDocument(new PlainPage(p)))
        .mapToLong(entry -> Long.parseLong(entry.getFileName().toString()))
        .forEach(availableDocuments::add);
  }

  @Override
  public Stream<Page> fetchDocuments(Query query) {
    return filter.filtrate(query)
        .mapToObj(id -> indexRoot.resolve(Long.toString(id)))
        .map(PlainPage::new);
  }

  @Override
  public int indexSize() {
    return availableDocuments.size();
  }

  @Override
  public double averageWordsPerPage() {
    return statistics.getAverageDocumentLength();
  }

  @Override
  public int pagesWithTerm(Term term) {
    return statistics.getNumberOfDocumentsWithWord().get(term.getNormalized());
  }

  @Override
  public long termCollectionFrequency(Term term) {
    return statistics.getNumberOfDocumentsWithWord().get(term.getNormalized());
  }

  @Override
  public int vocabularySize() {
    return statistics.getVocabularySize();
  }
}
