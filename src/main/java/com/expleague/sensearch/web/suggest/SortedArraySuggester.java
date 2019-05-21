package com.expleague.sensearch.web.suggest;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.expleague.sensearch.web.suggest.pool.LearnedSuggester.StringDoublePair;
import com.google.inject.Inject;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class SortedArraySuggester implements Suggester {

  public final int RETURN_LIMIT = 10;

  private final SortedMultigramsArray multigrams;
  private final PlainIndex index;
  

  private String printName;
  
  private BiFunction<List<Term>, List<Term>, Double> similarity;
  
  @Inject
  public SortedArraySuggester(Index index) {
    this.index = (PlainIndex) index;

    SuggestInformationLoader provider = index.getSuggestInformation();

    multigrams = new SortedMultigramsArray(provider.multigramFreqNorm);
    
    printName = "Sorted Array";
    
    similarity = (l1, l2) -> 1.0;
  }
  
  public SortedArraySuggester(Index index, BiFunction<List<Term>, List<Term>, Double> similarity, String printName) {
    this(index);
    this.printName = printName;
    
    this.similarity = similarity;
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
    return printName;
  }

  private List<String> getSuggestions(List<Term> terms) {

    if (terms.isEmpty()) {
      return Collections.emptyList();
    }

    int workCounter = 0;

    TreeSet<StringDoublePair> phraseProb = new TreeSet<>();

    l:  for(int intersectLength = 3; intersectLength >= 1; intersectLength--) {

      if (terms.size() < intersectLength) {
        continue;
      }

      List<Term> qcNonIntersect = terms.subList(0, terms.size() - intersectLength);
      List<Term> qc = terms.subList(0, terms.size() - 1);
      Term[] qt = terms.subList(terms.size() - intersectLength, terms.size()).toArray(new Term[0]);

      String qcNonIntersectText = qcNonIntersect
          .stream()
          .map(Term::text)
          .collect(Collectors.joining(" "));
      
      List<MultigramWrapper> endingPhrases = multigrams.getMatchingPhrases(qt);
      //System.out.println("number of selected phrases: " + endingPhrases.size());
      for (MultigramWrapper p : endingPhrases) {
        String phraseText = Arrays.asList(p.phrase).stream()
            .map(Term::text)
            .collect(Collectors.joining(" "));
        
        String suggestion = qcNonIntersectText.isEmpty() ? phraseText : (qcNonIntersectText + " " + phraseText);
        
        if (qc.size() > 0) {
          //phraseProb.add(new StringDoublePair(suggestion, VecTools.cosine(queryVec, index.vecByTerms(Arrays.asList(p.phrase)))));
          phraseProb.add(new StringDoublePair(suggestion, similarity.apply(qc, Arrays.asList(p.phrase))));
        } else {
          phraseProb.add(new StringDoublePair(suggestion, -Arrays.stream(p.phrase).mapToDouble(index::tfidf).sum()));
        }
        phraseProb.removeIf(it -> phraseProb.size() > RETURN_LIMIT);

        workCounter++;
        if (workCounter > 1e6) {
          System.err.println("Suggestor stopped by counter");
          break l;
        }
      }
/*
      if (phraseProb.size() > 5)
        break l;*/

    }

    return phraseProb.stream()
        .sorted((m1, m2) -> -Double.compare(m1.coeff, m2.coeff))
        .map(p -> p.phrase)
        .collect(Collectors.toList());
  }

}
