package com.expleague.sensearch.query;

import com.expleague.sensearch.core.Term;
import java.util.List;
import java.util.Map;

public interface Query {
  List<Term> terms();
  Map<Term, List<Term>> synonyms();

  String text();
}
