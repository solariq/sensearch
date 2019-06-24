package com.expleague.sensearch.donkey.utils;

import com.expleague.commons.seq.CharSeq;
import com.expleague.sensearch.donkey.writers.TermWriter;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class Dictionary implements AutoCloseable {

  private TermWriter termWriter;
  private TObjectIntMap<CharSeq> termToIntMap;
  private TIntObjectMap<CharSeq> intToTermMap;

  public Dictionary(TermWriter termWriter) {
    this.termWriter = termWriter;
    this.termToIntMap = new TObjectIntHashMap<>();
    this.intToTermMap = new TIntObjectHashMap<>();
  }

  public Dictionary(TermWriter termWriter, TObjectIntMap<CharSeq> termToIntMap,
      TIntObjectMap<CharSeq> intToTermMap) {
    this.termWriter = termWriter;
    this.termToIntMap = termToIntMap;
    this.intToTermMap = intToTermMap;
  }

  void addTerm(ParsedTerm term) {
    if (!termToIntMap.containsKey(term.word())) {
      termToIntMap.put(term.word(), term.wordId());
      intToTermMap.put(term.wordId(), term.word());
      termWriter.write(term);
    }
  }

  boolean contains(CharSeq word) {
    return termToIntMap.containsKey(word);
  }

  int get(CharSeq word) {
    return termToIntMap.get(word);
  }

  CharSeq get(int word) {
    return intToTermMap.get(word);
  }

  @Override
  public void close() {

  }
}
