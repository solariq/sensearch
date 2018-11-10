package com.expleague.sensearch.query.term;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.text.lemmer.LemmaInfo;

public interface Term {

  public CharSequence getRaw();

  public CharSequence getNormalized();

  public LemmaInfo getLemma();
}
