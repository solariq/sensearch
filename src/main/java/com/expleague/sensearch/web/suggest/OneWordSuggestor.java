package com.expleague.sensearch.web.suggest;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.math.vectors.VecTools;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class OneWordSuggestor implements Suggestor {

  public final int RETURN_LIMIT = 10;

  private final Map<Term, Double> unigramCoeff;

  private final ArrayList<Term> sortedUnigrams = new ArrayList<>();

  private PlainIndex index;

  @Inject
  public OneWordSuggestor(Index index) {
    this.index = (PlainIndex) index;

    SuggestInformationLoader provider = index.getSuggestInformation();
    unigramCoeff = provider.unigramCoeff;

    unigramCoeff.keySet().stream()
    .sorted((t1, t2) -> t1.text().toString().compareTo(t2.text().toString()))
    .forEach(t -> sortedUnigrams.add(t));;

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
    return res;
  }

  private int lowerBound(String qt) {
    int l = 0, r = sortedUnigrams.size();
    while (r - l > 1) {
      int m = (r - l) / 2 + l;
      boolean less;
      String atMTerm = sortedUnigrams.get(m).text().toString();
      if (atMTerm.length() >= qt.length()) {
        String atM = sortedUnigrams.get(m).text().toString().substring(0, qt.length());
        less = qt.compareTo(atM) > 0;
      } else {
        less = qt.compareTo(atMTerm) > 0;
      }
      if (less) {
        l = m;
      } else {
        r = m;
      }
    }
    return r;
  }

  private int upperBound(String qt) {
    int l = 0, r = sortedUnigrams.size();
    while (r - l > 1) {
      int m = (r - l) / 2 + l;
      boolean less;
      String atMTerm = sortedUnigrams.get(m).text().toString();
      if (atMTerm.length() >= qt.length()) {
        String atM = sortedUnigrams.get(m).text().toString().substring(0, qt.length());
        less = qt.compareTo(atM) >= 0;
      } else {
        less = qt.compareTo(atMTerm) >= 0;
      }
      if (less) {
        l = m;
      } else {
        r = m;
      }
    }
    return l;
  }

  private class TermDoublePair implements Comparable<TermDoublePair>{
    public TermDoublePair(Term term, double val) {
      this.term = term;
      this.val = val;
    }
    public Term term;
    public double val;

    @Override
    public int compareTo(TermDoublePair o) {
      return Double.compare(val, o.val);
    }
  }

  private List<String> getSuggestions(List<Term> terms) {

    if (terms.isEmpty()) {
      return Collections.emptyList();
    }

    String qt = terms.get(terms.size() - 1).text().toString();
    List<Term> qc = terms.subList(0, terms.size() - 1);

    String qcText = qc
        .stream()
        .map(Term::text)
        .collect(Collectors.joining(" "));

    Vec queryVec = index.vecByTerms(qc);

    TreeSet<TermDoublePair> phraseProb = new TreeSet<>();

    if (qt.length() > 1) {
      int lb = lowerBound(qt), ub = upperBound(qt);
      System.out.println(lb + " "+ ub);
      for (int i = lb; i <= ub; i++) {
        Term p = sortedUnigrams.get(i);
        if (qc.size() > 0) {
          phraseProb.add(new TermDoublePair(p, VecTools.cosine(queryVec, index.vecByTerms(Arrays.asList(p)))));
        } else {
          phraseProb.add(new TermDoublePair(p, unigramCoeff.get(p)));
        }
        phraseProb.removeIf(it -> phraseProb.size() > RETURN_LIMIT);
      }

      return phraseProb.stream()
          .sorted(Comparator.reverseOrder())
          .map(p -> p.term)
          .map(t -> qcText + " " + t.text())
          .collect(Collectors.toList());

    } else {
      return index.nearestTerms(queryVec)
          .filter(t -> t.text().toString().startsWith(qt))
          .limit(RETURN_LIMIT)
          .map(t -> qcText + " " + t.text().toString())
          .collect(Collectors.toList());
    }

  }

}
