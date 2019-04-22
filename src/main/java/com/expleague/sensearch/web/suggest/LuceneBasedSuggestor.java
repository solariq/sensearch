package com.expleague.sensearch.web.suggest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;


public class LuceneBasedSuggestor implements Suggestor {

  private final PlainIndex index;

  private final AnalyzingInfixSuggester luceneSuggestor;

  private String termsToString(Term[] terms) {
    return Arrays.stream(terms)
        .map(Term::text)
        .collect(Collectors.joining(" "));
  }

  //@Inject
  public LuceneBasedSuggestor(Index index, Path indexRoot) throws IOException {
    this.index = (PlainIndex) index;
    SuggestInformationLoader sgil = index.getSuggestInformation();
    
    luceneSuggestor = new AnalyzingInfixSuggester(
        FSDirectory.open(indexRoot.resolve("luceneSuggest")),
        new StandardAnalyzer());
     
  }
  
  public static void main(String[] args) throws IOException {
    final Path tmpDir = Files.createTempDirectory("testFolder");
    final Analyzer analyzer = new StandardAnalyzer();

    // Populate the data first.
    {
      Directory dir = FSDirectory.open(tmpDir);

      AnalyzingInfixSuggester luceneSuggestor = new AnalyzingInfixSuggester(dir, analyzer);
      luceneSuggestor.add(new BytesRef("one"), null, 0, null);
      luceneSuggestor.commit();
      luceneSuggestor.close();
    }
    {
      Directory dir = FSDirectory.open(tmpDir);

      AnalyzingInfixSuggester luceneSuggestor = new AnalyzingInfixSuggester(dir, analyzer);

      System.out.println(luceneSuggestor.lookup("", false, 10).size());
      luceneSuggestor.lookup("o", false, 10).forEach(lr -> {
        System.out.println("suggest" + lr.key);
      });
      luceneSuggestor.close();
    }
    System.out.println( "Hello World!" );
  }

  @Override
  public String getName() {
    return "Lucene " + luceneSuggestor.getClass().getSimpleName();
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
