package com.expleague.sensearch.index.plain;

import com.expleague.commons.math.vectors.Vec;
import com.expleague.commons.seq.CharSeq;
import com.expleague.sensearch.core.PartOfSpeech;
import com.expleague.sensearch.core.Term;
import java.util.Collections;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public class IndexTerm implements Term {
  private final PlainIndex owner;
  private final CharSeq text;
  private final IndexTerm lemma;
  private final long id;
  private final PartOfSpeech partOfSpeech;

  public IndexTerm(
      PlainIndex owner, CharSeq text, long id, IndexTerm lemma, PartOfSpeech partOfSpeech) {
    this.owner = owner;
    this.text = text;
    this.id = id;
    this.lemma = lemma;
    this.partOfSpeech = partOfSpeech;
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
  public Vec vec() {
    return owner.vecByTerms(Collections.singletonList(this));
  }

  @Override
  public Stream<Term> synonyms() {
    return owner.synonyms(this);
  }

  @Override
  public Stream<Term> synonyms(double synonymThreshold) {
    return owner.synonyms(this, synonymThreshold);
  }

  @Override
  public Stream<TermAndDistance> synonymsWithDistance(double synonymThreshold) {
    return owner.synonymsWithDistance(this, synonymThreshold);
  }

  @Override
  public Stream<TermAndDistance> synonymsWithDistance() {
    return owner.synonymsWithDistance(this);
  }

  @Override
  public int documentFreq() {
    return owner.documentFrequency(this);
  }

  @Override
  public int documentLemmaFreq() {
    return owner.documentLemmaFrequency(this);
  }

  @Override
  public int freq() {
    return owner.termFrequency(this);
  }

  @Nullable
  @Override
  public PartOfSpeech partOfSpeech() {
    return partOfSpeech;
  }

  public long id() {
    return id;
  }


  public static class IndexTermAndDistance implements TermAndDistance {

    private final Term term;
    private final double distance;

    public IndexTermAndDistance(Term term, double distance) {
      this.term = term;
      this.distance = distance;
    }

    @Override
    public Term term() {
      return term;
    }

    @Override
    public double distance() {
      return distance;
    }
  }
}
