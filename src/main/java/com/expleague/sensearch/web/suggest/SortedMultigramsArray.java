package com.expleague.sensearch.web.suggest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.expleague.sensearch.core.Term;

public class SortedMultigramsArray {
  private final ArrayList<MultigramWrapper> sortedMultigrams = new ArrayList<>();

  private final Comparator<Term[]> bsCmp = new BinSearchComparator();
  private final Comparator<Term[]> sortCmp = new PhraseSortingComparator();

  public SortedMultigramsArray(Collection<MultigramWrapper> multList) {
    sortedMultigrams.addAll(multList);
    Collections.sort(sortedMultigrams,
        (m1, m2) -> sortCmp.compare(m1.phrase, m2.phrase)
        //(m1, m2) -> m1.toString().compareTo(m2.toString())
        );
    System.err.println("sortedMultigrams: " + sortedMultigrams.size());
  }

  public SortedMultigramsArray(Map<Term[], Double> multList) {
    multList.forEach((t, d) -> sortedMultigrams.add(new MultigramWrapper(t, d)));
    Collections.sort(sortedMultigrams,
        (m1, m2) -> sortCmp.compare(m1.phrase, m2.phrase)
        //(m1, m2) -> m1.toString().compareTo(m2.toString())
        );
  }

  private int binSearchPosition(Term[] words, boolean upperBound) {
    //String words_s = new MultigramWrapper(words, 0).toString();
    int l = 0, r = sortedMultigrams.size();
    while (r - l > 1) {
      int m = (r - l) / 2 + l;

      //int c = words_s.compareTo(sortedMultigrams.get(m).toString());
      int c = bsCmp.compare(words, sortedMultigrams.get(m).phrase);
      if (c > 0 || (upperBound && c == 0)) {
        l = m;
      } else {
        r = m;
      }
    }
    return r;
  }

  public List<MultigramWrapper> getMatchingPhrases(Term[] lastWords) {

    //System.out.println("Array: " + Arrays.stream(lastWords).map(t -> t.text()).collect(Collectors.joining("#")));
    int lowerBound = binSearchPosition(lastWords, false);

    int upperBound = binSearchPosition(lastWords, true);

    //System.out.println(lowerBound + " " + upperBound + " " + sortedMultigrams.size());
    return sortedMultigrams.subList(lowerBound, upperBound);
  }
  
  public int[] getBounds(Term[] lastWords) {
    int lowerBound = binSearchPosition(lastWords, false);

    int upperBound = binSearchPosition(lastWords, true);
    
    return new int[] {lowerBound, upperBound};
  }
  
  public MultigramWrapper get(int idx) {
    return sortedMultigrams.get(idx);
  }

}
