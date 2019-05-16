package com.expleague.sensearch.web.suggest;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.store.FSDirectory;
import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.web.suggest.pool.LearnedSuggester.StringDoublePair;
import com.google.inject.Inject;

public class OneWordLuceneTFIDF implements Suggestor {
  public final int RETURN_LIMIT = 10;

  public final static String filePrefix = "prefix_sugg";
  
  private final AnalyzingSuggester suggester;
  
  private final PlainIndex index;
  
  public final static Path storePath = Paths.get("luceneSuggestPrefix/store");
  
  @Inject
  public OneWordLuceneTFIDF(Index index, Path suggestIndexRoot) throws IOException {
    this.index = (PlainIndex) index;
    
    suggester = new AnalyzingSuggester(
        //FSDirectory.open(suggestIndexRoot.resolve(storePath).getParent()),
        //filePrefix,
        new StandardAnalyzer());
    
    suggester.load(new FileInputStream(suggestIndexRoot.resolve(storePath).toFile()));
  }

  @Override
  public List<String> getSuggestions(String searchString) {
    List<String> res = null;
    try {
      res = getSuggestions(index.parse(searchString.toLowerCase())
          .collect(Collectors.toList()));

    } catch (Exception e) {
      e.printStackTrace();
    }

    //System.out.println("returning number suggestions: " + res.size());
    return res;
  }
  
  @Override
  public String getName() {
    return "One Word Lucene TDIDF";
  }

  private String termsToString(Term[] terms) {
    return Arrays.stream(terms).map(t -> t.text().toString())
        .collect(Collectors.joining(" "));
  }
  
  private List<String> getSuggestions(List<Term> terms) throws IOException {

    if (terms.isEmpty()) {
      return Collections.emptyList();
    }

    TreeSet<StringDoublePair> phraseProb = new TreeSet<>();

    for (int intersectLength = 3; intersectLength >= 1; intersectLength--) {

      if (terms.size() < intersectLength) {
        continue;
      }

      List<Term> qc = terms.subList(0, terms.size() - intersectLength);
      String qcText = qc.stream()
          .map(Term::text)
          .collect(Collectors.joining(" "));
      
      Term[] qt = terms.subList(terms.size() - intersectLength, terms.size()).toArray(new Term[0]);

      List<LookupResult> endingPhrases = suggester.lookup(termsToString(qt), false, 1000000);
      
      //System.out.println("number of selected phrases: " + endingPhrases.size());
      for (LookupResult p : endingPhrases) {
        Term[] phrase = index.parse(p.key).toArray(Term[]::new);
/*        if (phrase.length > intersectLength) {
          continue;
        }*/
        
        String suggestion = qcText.isEmpty() ? p.key.toString() : (qcText + " " + p.key);
        
        phraseProb.add(new StringDoublePair(suggestion, Arrays.stream(phrase).mapToDouble(t -> ((PlainIndex )index).tfidf(t)).average().orElse(0)));
        phraseProb.removeIf(it -> phraseProb.size() > RETURN_LIMIT);


      }


    }

    return phraseProb.stream()
        .sorted((m1, m2) -> -Double.compare(m1.coeff, m2.coeff))
        .map(p -> p.phrase)
        .collect(Collectors.toList());
  }
}
