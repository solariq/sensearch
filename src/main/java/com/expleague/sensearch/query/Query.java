package com.expleague.sensearch.query;

import com.expleague.sensearch.core.Term;

import java.util.List;

public interface Query {

  List<Term> terms();
}
