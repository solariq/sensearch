package com.expleague.sensearch.index.plain;

import com.expleague.commons.seq.CharSeq;
import com.expleague.sensearch.core.Term;

import java.util.stream.Stream;

public class IndexTerm implements Term {
  private final PlainIndex owner;
  private final CharSeq text;
  private final IndexTerm lemma;
  private final long id;

  public IndexTerm(PlainIndex owner, CharSeq text, long id, IndexTerm lemma) {
    this.owner = owner;
    this.text = text;
    this.id = id;
    this.lemma = lemma;
  }

  @Override
  public CharSequence text() {
    return text;
  }

  @Override
  public Term lemma() {
    return lemma != null ? lemma : this;
  }

  @Override
  public Stream<Term> synonyms() {
    return owner.synonyms(this);
  }

  @Override
  public int documentFreq() {
    return owner.documentFrequency(this);
  }

  @Override
  public int freq() {
    return owner.termFrequency(this);
  }

  public long id() {
    return id;
  }
}
