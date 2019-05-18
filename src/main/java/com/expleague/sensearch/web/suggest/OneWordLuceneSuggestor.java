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

public class OneWordLuceneSuggestor implements Suggestor {
  public final int RETURN_LIMIT = 10;

  public final static String filePrefix = "prefix_sugg";
  
  private final AnalyzingSuggester suggester;
  
  private final PlainIndex index;
  
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

  @Inject
  public OneWordLuceneSuggestor(Index index, Path suggestIndexRoot) throws IOException {
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
    return "One Word Lucene: cosine and links";
  }

  private String termsToString(Term[] terms) {
    return Arrays.stream(terms).map(t -> t.text().toString())
        .collect(Collectors.joining(" "));
  }
  
  private List<String> getSuggestions(List<Term> terms) throws IOException {

    if (terms.isEmpty()) {
      return Collections.emptyList();
    }

    //TreeSet<MultigramWrapper> phraseProb = new TreeSet<>(mwCmp);
    TreeSet<StringDoublePair> phraseProb = new TreeSet<>();


    l:  for (int intersectLength = 3; intersectLength >= 1; intersectLength--) {

      if (terms.size() < intersectLength) {
        continue;
      }

      List<Term> qcNonIntersecting = terms.subList(0, terms.size() - intersectLength);
      String qcText = qcNonIntersecting.stream()
          .map(Term::text)
          .collect(Collectors.joining(" "));
      
      Term[] qt = terms.subList(terms.size() - intersectLength, terms.size()).toArray(new Term[0]);

      Vec queryVec = index.vecByTerms(terms.subList(0, terms.size() - 1));
      //VecTools.normalizeL1(queryVec);

      //Vec wQueryVec = index.weightedVecByTerms(qc);

      List<LookupResult> endingPhrases = suggester.lookup(termsToString(qt), false, 1000000);
      
      //System.out.println("number of selected phrases: " + endingPhrases.size());
      for (LookupResult p : endingPhrases) {
        Term[] phrase = index.parse(p.key).toArray(Term[]::new);
/*        if (phrase.length > intersectLength) {
          continue;
        }*/
        
        String suggestion = qcText.isEmpty() ? p.key.toString() : (qcText + " " + p.key);
        
        Vec phraseVec = index.vecByTerms(Arrays.asList(phrase));
        //normalizeL1(phraseVec);
        
        double cosine = VecTools.cosine(queryVec, phraseVec);

        System.err.println("one word cosine: " + cosine + " " + VecTools.l1(queryVec) + " " + VecTools.l1(phraseVec));

        if (qcNonIntersecting.size() > 0) {
          phraseProb.add(new StringDoublePair(suggestion, cosine));
          //phraseProb.add(new StringDoublePair(qcText + " " + p.key, VecTools.cosine(wQueryVec, index.weightedVecByTerms(Arrays.asList(phrase)))));
        } else {
          phraseProb.add(new StringDoublePair(suggestion, p.value));
        }
        //phraseProb.add(new MultigramWrapper(phrase, Arrays.stream(phrase).mapToDouble(t -> ((PlainIndex )index).tfidf(t)).average().orElse(0)));
        phraseProb.removeIf(it -> phraseProb.size() > RETURN_LIMIT);

      }

    }

    return phraseProb.stream()
        .sorted((m1, m2) -> -Double.compare(m1.coeff, m2.coeff))
        .map(p -> p.phrase)
        .collect(Collectors.toList());
  }
}
