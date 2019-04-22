package com.expleague.sensearch.web.suggest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.FSDirectory;


public class RawLuceneSuggestor implements Suggestor {

  private final AnalyzingInfixSuggester luceneSuggestor;

  //@Inject
  public RawLuceneSuggestor(Path indexRoot) throws IOException {
    
    luceneSuggestor = new AnalyzingInfixSuggester(
        FSDirectory.open(indexRoot.resolve("luceneSuggest")),
        new RussianAnalyzer());
     
  }

  @Override
  public String getName() {
    return "Lucene " + RawLuceneSuggestor.class.getSimpleName();
  }
  
  @Override
  public List<String> getSuggestions(String searchString) {
    try {
      return luceneSuggestor.lookup(searchString, false, 10)
          .stream()
          .map(lr -> lr.key.toString())
          .collect(Collectors.toList());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

}
