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

public class OneWordLuceneSuggestor implements Suggestor{
  public final int RETURN_LIMIT = 10;

  public final static String filePrefix = "prefix_sugg";
  
  private final AnalyzingSuggester suggester;
  
  private final Index index;
  
  public final static Path storePath = Paths.get("luceneSuggestPrefix/store");
  
  private final Comparator<MultigramWrapper> mwCmp = new Comparator<MultigramWrapper>() {

    Comparator<Term[]> termsCmp = new PhraseSortingComparator();

    @Override
    public int compare(MultigramWrapper o1, MultigramWrapper o2) {
      int res = Double.compare(o1.coeff, o2.coeff);
      if (res != 0)
        return res;

      return termsCmp.compare(o1.phrase, o2.phrase);
    }

  };

  public OneWordLuceneSuggestor(Index index, Path indexRoot) throws IOException {
    this.index = index;
    
    suggester = new AnalyzingSuggester(
        FSDirectory.open(indexRoot.resolve(storePath).getParent()),
        filePrefix,
        new StandardAnalyzer());
    
    suggester.load(new FileInputStream(indexRoot.resolve(storePath).toFile()));
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
    return "One Word Lucene";
  }

  private String termsToString(Term[] terms) {
    return Arrays.stream(terms).map(t -> t.text().toString())
        .collect(Collectors.joining(" "));
  }
  
  private List<String> getSuggestions(List<Term> terms) throws IOException {

    if (terms.isEmpty()) {
      return Collections.emptyList();
    }

    int workCounter = 0;

    TreeSet<MultigramWrapper> phraseProb = new TreeSet<>(mwCmp);

    l:  for (int intersectLength = 3; intersectLength >= 1; intersectLength--) {

      if (terms.size() < intersectLength) {
        continue;
      }

      List<Term> qc = terms.subList(0, terms.size() - intersectLength);
      Term[] qt = terms.subList(terms.size() - intersectLength, terms.size()).toArray(new Term[0]);

      Vec queryVec = index.vecByTerms(qc);

      List<LookupResult> endingPhrases = suggester.lookup(termsToString(qt), false, 10000);
      
      //System.out.println("number of selected phrases: " + endingPhrases.size());
      for (LookupResult p : endingPhrases) {
        Term[] phrase = index.parse(p.key).toArray(Term[]::new);
        if (phrase.length > intersectLength) {
          continue;
        }
        
        if (qc.size() > 0) {
          phraseProb.add(new MultigramWrapper(phrase, VecTools.cosine(queryVec, index.vecByTerms(Arrays.asList(phrase)))));
        } else {
          phraseProb.add(new MultigramWrapper(phrase, p.value));
        }
        phraseProb.removeIf(it -> phraseProb.size() > RETURN_LIMIT);

        workCounter++;
        if (workCounter > 1e6) {
          System.err.println("Suggestor stopped by counter");
          break l;
        }
      }

      if (phraseProb.size() > 5)
        break l;

    }

    return phraseProb.stream()
        .sorted((m1, m2) -> -Double.compare(m1.coeff, m2.coeff))
        .map(p -> {
          String qcText = terms.subList(0, terms.size() - p.phrase.length)
              .stream()
              .map(Term::text)
              .collect(Collectors.joining(" "));
          return qcText.isEmpty() ? p.toString() : qcText + " " + p;
        })
        .collect(Collectors.toList());
  }
}
