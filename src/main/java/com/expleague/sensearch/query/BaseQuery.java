package com.expleague.sensearch.query;

import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;

import java.util.List;
import java.util.stream.Collectors;

public class BaseQuery implements Query {
  private List<Term> terms;

  public BaseQuery(List<Term> terms) {
    this.terms = terms;
  }

  public static Query create(String input, Index index) {
    return new BaseQuery(index.parse(input).collect(Collectors.toList()));
  }

  @Override
  public List<Term> terms() {
    return this.terms;
  }
}
