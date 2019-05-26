package com.expleague.sensearch.web.suggest;

import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.google.inject.Inject;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class LinksSuggester implements Suggester {

  public final int RETURN_LIMIT = 10;

  private final SortedMultigramsArray multigrams;
  private final PlainIndex index;
  
  static class WeightIdxIntersect implements Comparable<WeightIdxIntersect>{
    public final double weight;
    public final int idx;
    public final int intersect;
    
    public WeightIdxIntersect(double weight, int idx, int intersect) {
      this.weight = weight;
      this.idx = idx;
      this.intersect = intersect;
    }
    
    @Override
    public int compareTo(WeightIdxIntersect o) {
      return Double.compare(weight, o.weight);
    }
    
  }
  
  @Inject
  public LinksSuggester(Index index) {
    this.index = (PlainIndex) index;

    SuggestInformationLoader provider = index.getSuggestInformation();

    multigrams = new SortedMultigramsArray(provider.multigramFreqNorm);
        
  }

  @Override
  public List<String> getSuggestions(String searchString) {
    //System.err.println("suggest for " + searchString + " requested");
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
    return "Links Suggester";
  }

  private List<String> getSuggestions(List<Term> terms) {

    if (terms.isEmpty()) {
      return Collections.emptyList();
    }

    int workCounter = 0;

    TreeSet<WeightIdxIntersect> phraseProb = new TreeSet<>();

    l:  for(int intersectLength = 3; intersectLength >= 1; intersectLength--) {

      if (terms.size() < intersectLength) {
        continue;
      }

      Term[] qt = terms.subList(terms.size() - intersectLength, terms.size()).toArray(new Term[0]);

      
      int[] bounds = multigrams.getBounds(qt);
      //System.out.println("number of selected phrases: " + endingPhrases.size());
      for (int i = bounds[0]; i < bounds[1]; i++) {
        
        double coeff = multigrams.get(i).coeff;
        //System.err.println(coeff);
        phraseProb.add(new WeightIdxIntersect(coeff, i, intersectLength));
        phraseProb.removeIf(it -> phraseProb.size() > RETURN_LIMIT);

        workCounter++;
        if (workCounter > 1e6) {
          System.err.println("Suggestor stopped by counter");
          break l;
        }
      }

    }

    return phraseProb.stream()
        .sorted(Comparator.reverseOrder())
        .map(p -> {
          String pref = termsToString(terms.subList(0, terms.size() - p.intersect));
          String suff = termsToString(multigrams.get(p.idx).phrase);
         return wordsConcat(pref, suff);
        })
        .collect(Collectors.toList());
  }

}

