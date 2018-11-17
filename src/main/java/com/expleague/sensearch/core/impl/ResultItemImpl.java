package com.expleague.sensearch.core.impl;

import com.expleague.commons.util.Pair;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.snippet.Segment;
import java.net.URI;
import java.util.List;

public class ResultItemImpl implements ResultItem {

  private final URI reference;
  private final CharSequence title;
  private final List<Pair<CharSequence, List<Segment>>> passages;
  private final double score;

  public ResultItemImpl(
      URI reference,
      CharSequence title,
      List<Pair<CharSequence, List<Segment>>> passages,
      double score) {
    this.reference = reference;
    this.title = title;
    this.passages = passages;
    this.score = score;
  }


  @Override
  public URI reference() {
    return reference;
  }

  @Override
  public CharSequence title() {
    return title;
  }

  @Override
  public List<Pair<CharSequence, List<Segment>>> passages() {
    return passages;
  }

  @Override
  public double score() {
    return score;
  }
}
