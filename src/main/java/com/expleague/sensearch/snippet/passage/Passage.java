package com.expleague.sensearch.snippet.passage;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Term;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Maxim on 10.10.2018. Email: alvinmax@mail.ru
 */
public class Passage {

  private final List<Term> words;
  private final Page owner;
  private int id;

  public Passage(List<Term> terms, Page owner) {
    this.words = terms;
    this.owner = owner;
  }

  public int id() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Stream<Term> words() {
    return words.stream();
  }

  public CharSequence sentence() {
    StringBuilder res = new StringBuilder();
    words.forEach(t -> res.append(t.text()));
    return res;
  }

  public Page owner() {
    return owner;
  }
}
