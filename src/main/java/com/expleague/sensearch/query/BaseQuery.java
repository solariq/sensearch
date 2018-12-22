package com.expleague.sensearch.query;

import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public class BaseQuery implements Query {
  private final Map<Term, List<Term>> synonyms;
  private List<Term> terms;
  private String text;

  private BaseQuery(String input, List<Term> terms) {
    this.terms = terms;
    this.synonyms = terms.stream()
        .map(term -> Pair.of(term, term.synonyms().collect(Collectors.toList())))
        .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
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
  public Map<Term, List<Term>> synonyms() {
    return synonyms;
  }

  @Override
  public String text() {
    return text;
  }
}
