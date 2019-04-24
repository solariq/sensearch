package com.expleague.sensearch.query;

import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.core.Term.TermAndDistance;
import com.expleague.sensearch.index.Index;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public class BaseQuery implements Query {

  public static final double SYNONYM_THRESHOLD = 0.75;
  public static final int MAX_SYNONYMS = 300;

  private Map<Term, List<TermAndDistance>> synonyms;
  private final List<Term> terms;
  private final String text;

  private BaseQuery(String input, List<Term> terms) {
    this.terms = terms;
    this.text = input;
  }

  public static Query create(String input, Index index) {
    return new BaseQuery(input, index.parse(input).collect(Collectors.toList()));
  }

  @Override
  public List<Term> terms() {
    return this.terms;
  }

  @Override
  public Map<Term, List<TermAndDistance>> synonymsWithDistance() {
    if (synonyms != null) {
      return synonyms;
    }
    synchronized (this) {
      if (synonyms == null) {
        synonyms =
            terms
                .stream()
                .map(
                    term ->
                        Pair.of(
                            term,
                            term.synonymsWithDistance()
                                .limit(MAX_SYNONYMS)
                                .collect(Collectors.toList())))
                .collect(Collectors.toSet())
                .stream()
                .collect(
                    Collectors.toMap(
                        Pair::getLeft,
                        Pair::getRight,
                        (x, y) -> {
                          System.out.println(x + " " + y);
                          return x;
                        }));
      }
      return synonyms;
    }
  }

  @Override
  public Map<Term, List<Term>> synonyms() {
    return synonymsWithDistance()
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                Entry::getKey,
                x ->
                    x.getValue().stream().map(TermAndDistance::term).collect(Collectors.toList())));
  }

  @Override
  public String text() {
    return text;
  }
}
