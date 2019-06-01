package com.expleague.sensearch.web.suggest;

import java.util.Comparator;
import com.expleague.sensearch.core.Term;

public class PhraseSortingComparator implements Comparator<Term[]> {

  protected int compare(String[] words, String[] line) {
/*
    if (words.length < line.length)
      return -1;

    if (words.length > line.length)
      return 1;
*/
    int len = Math.min(words.length, line.length);

    for (int i = 0; i < len; i++) {
      int r = words[i].compareTo(line[i]);
      if (r != 0) {
        return r;
      }
    }

    return Integer.compare(words.length, line.length);
  }

  private String[] termsToStringArr(Term[] terms) {
    String[] res = new String[terms.length];

    for (int i = 0; i < terms.length; i++) {
      res[i] = terms[i].text().toString();
    }

    return res;
  }

  @Override
  public int compare(Term[] words, Term[] line) {
    return compare(termsToStringArr(words), termsToStringArr(line));
  }


}
