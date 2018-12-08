package com.expleague.sensearch.index.plain;

import com.expleague.commons.seq.CharSeq;
import com.expleague.commons.text.lemmer.PartOfSpeech;
import com.expleague.sensearch.core.Term;
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

  @Nullable
  @Override
  public PartOfSpeech partOfSpeech() {
    return partOfSpeech;
  }

  public long id() {
    return id;
  }
}
