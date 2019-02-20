package com.expleague.sensearch.snippet.passage;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.snippet.Segment;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Maxim on 10.10.2018. Email: alvinmax@mail.ru
 */
public class Passage {

  private final Page owner;
  private final CharSequence sentence;
  private final List<Term> words;
  private double rating;
  private int id;

  public Passage(Page page, CharSequence sentence, List<Term> terms) {
    this.owner = page;
    this.sentence = sentence;
    this.words = terms;
    this.rating = 0;
  }

  public double rating() {
    return rating;
  }

  public void setRating(double rating) {
    this.rating = rating;
  }

  public int id() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public CharSequence sentence() {
    return sentence;
  }

  public Stream<Term> words() {
    return words.stream();
  }

  public Page owner() {
    return owner;
  }
}
