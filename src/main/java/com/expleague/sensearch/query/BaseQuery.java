package com.expleague.sensearch.query;

import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.query.term.BaseTerm;
import com.expleague.sensearch.query.term.Term;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class BaseQuery implements Query {

  private List<Term> terms;

  public BaseQuery(CharSequence queryString, Lemmer lemmer) {
    //todo replace for "smart" tokenizer when it zavezut
    String regex = " ";
    Pattern pattern = Pattern.compile(regex);
    terms = new ArrayList<>();

    for (CharSequence word : pattern.split(queryString)) {
      this.terms.add(new BaseTerm(lemmer.myStem.parse(word).get(0)));
    }

  }

  @Override
  public List<Term> getTerms() {
    return this.terms;
  }

}