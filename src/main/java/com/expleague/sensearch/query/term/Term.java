package com.expleague.sensearch.query.term;

import com.expleague.commons.math.vectors.Vec;

public interface Term {

  public CharSequence getRaw();

  public CharSequence getNormalized();

  public Vec getVector();
}
