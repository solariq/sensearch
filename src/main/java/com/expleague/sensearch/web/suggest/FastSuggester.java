package com.expleague.sensearch.web.suggest;

import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.index.plain.PlainIndex;
import com.google.inject.Inject;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class FastSuggester implements Suggester {

  public final int RETURN_LIMIT = 10;

  private final SortedMultigramsArray multigrams;
  private final PlainIndex index;

  private final SqrtNRMQ rmq;

  private final Random rnd = new Random(0);

  class WeightIdxIntersect implements Comparable<WeightIdxIntersect>{
    public final double weight;
    public final int idx;
    public final int intersect;
    public final int id;

    public WeightIdxIntersect(double weight, int idx, int intersect) {
      this.weight = weight;
      this.idx = idx;
      this.intersect = intersect;
      id = rnd.nextInt();
    }

    @Override
    public final int compareTo(WeightIdxIntersect o) {
      int res = Double.compare(weight, o.weight);
      if (res != 0)
        return res;
      return Integer.compare(id, o.id);
    }

  }

  @Inject
  public FastSuggester(Index index) {
    this.index = (PlainIndex) index;

    SuggestInformationLoader provider = index.getSuggestInformation();

    multigrams = new SortedMultigramsArray(provider.multigramFreqNorm);

    //System.err.println("Multigrams array was built");
    rmq = new SqrtNRMQ(RETURN_LIMIT, multigrams.getList().stream().mapToDouble(mw -> mw.coeff));
    //System.err.println("RMQ was built");
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
    return "RMQ Links Suggester";
  }

  private List<String> getSuggestions(List<Term> terms) {

    if (terms.isEmpty()) {
      return Collections.emptyList();
    }


    TreeSet<WeightIdxIntersect> phraseProb = new TreeSet<>();

    for(int intersectLength = 3; intersectLength >= 1; intersectLength--) {

      if (terms.size() < intersectLength) {
        continue;
      }

      Term[] qt = terms.subList(terms.size() - intersectLength, terms.size()).toArray(new Term[0]);

      int[] bounds = multigrams.getBounds(qt);

      //System.err.println(bounds[0] + " " + bounds[1]);
      int[] maxIdxs = rmq.getMaximumIdxs(bounds[0], bounds[1]);
      for (int i : maxIdxs) {
        phraseProb.add(new WeightIdxIntersect(multigrams.get(i).coeff, i, intersectLength));
      }

    }

    return phraseProb.stream()
        .sorted(Comparator.reverseOrder())
        .limit(RETURN_LIMIT)
        .map(p -> {
          String pref = termsToString(terms.subList(0, terms.size() - p.intersect));
          String suff = termsToString(multigrams.get(p.idx).phrase);
          return wordsConcat(pref, suff);
        })
        .collect(Collectors.toList());
  }

}

