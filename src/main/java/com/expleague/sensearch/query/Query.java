package com.expleague.sensearch.query;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.sensearch.query.term.Term;
import java.util.List;

public interface Query {

  public List<Term> getTerms();

}
