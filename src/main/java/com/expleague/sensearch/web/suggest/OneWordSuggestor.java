package com.expleague.sensearch.web.suggest;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.google.inject.Inject;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class OneWordSuggestor implements Suggestor {

  public final int RETURN_LIMIT = 10;

  private final SortedMultigramsArray multigrams;
  private final PlainIndex index;
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
  public OneWordSuggestor(Index index) {
    this.index = (PlainIndex) index;

    SuggestInformationLoader provider = index.getSuggestInformation();

    multigrams = new SortedMultigramsArray(provider.multigramFreqNorm);
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
    return "One Word";
  }

  private List<String> getSuggestions(List<Term> terms) {

    if (terms.isEmpty()) {
      return Collections.emptyList();
    }

    int workCounter = 0;

    TreeSet<MultigramWrapper> phraseProb = new TreeSet<>(mwCmp);

    l:  for(int intersectLength = 3; intersectLength >= 1; intersectLength--) {

      if (terms.size() < intersectLength) {
        continue;
      }

      List<Term> qc = terms.subList(0, terms.size() - intersectLength);
      Term[] qt = terms.subList(terms.size() - intersectLength, terms.size()).toArray(new Term[0]);

      Vec queryVec = index.vecByTerms(qc);

      List<MultigramWrapper> endingPhrases = multigrams.getMatchingPhrases(qt);
      //System.out.println("number of selected phrases: " + endingPhrases.size());
      for (MultigramWrapper p : endingPhrases) {
        if (qc.size() > 0) {
          phraseProb.add(new MultigramWrapper(p.phrase, VecTools.cosine(queryVec, index.vecByTerms(Arrays.asList(p.phrase)))));
        } else {
          phraseProb.add(p);
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
        .map(p -> {
          String qcText = terms.subList(0, terms.size() - p.phrase.length)
              .stream()
              .map(Term::text)
              .collect(Collectors.joining(" "));
          return (qcText.isEmpty() ? p.toString() : qcText + " " + p);// + " " + p.coeff;
        })
        .collect(Collectors.toList());
  }

}
