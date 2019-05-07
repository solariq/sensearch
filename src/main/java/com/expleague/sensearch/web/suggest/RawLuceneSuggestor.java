package com.expleague.sensearch.web.suggest;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.FSDirectory;


public class RawLuceneSuggestor implements Suggestor {

  private final AnalyzingInfixSuggester luceneSuggestor;

  public static final Path storePath = Paths.get("luceneSuggest");
  
  //@Inject
  public RawLuceneSuggestor(Path indexRoot) throws IOException {
    
    luceneSuggestor = new AnalyzingInfixSuggester(
        FSDirectory.open(indexRoot.resolve(storePath)),
        new StandardAnalyzer());
     
  }

  @Override
  public String getName() {
    return RawLuceneSuggestor.class.getSimpleName();
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
