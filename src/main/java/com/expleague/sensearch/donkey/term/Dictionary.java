package com.expleague.sensearch.donkey.term;

import com.expleague.commons.seq.CharSeq;
import com.expleague.sensearch.donkey.writers.Writer;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.IOException;

public class Dictionary implements AutoCloseable {

  private Writer<ParsedTerm> termWriter;
  private TObjectIntMap<CharSeq> termToIntMap;
  private TIntObjectMap<CharSeq> intToTermMap;

  public Dictionary(Writer<ParsedTerm> termWriter) {
    this.termWriter = termWriter;
    this.termToIntMap = new TObjectIntHashMap<>();
    this.intToTermMap = new TIntObjectHashMap<>();
  }

  public Dictionary(Writer<ParsedTerm> termWriter, TObjectIntMap<CharSeq> termToIntMap,
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
    try {
      termWriter.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
