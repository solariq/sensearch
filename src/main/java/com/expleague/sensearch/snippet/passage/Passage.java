package com.expleague.sensearch.snippet.passage;

import com.expleague.sensearch.core.Term;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created by Maxim on 10.10.2018. Email: alvinmax@mail.ru
 */
public class Passage {

  private final CharSequence sentence;
  private final List<Term> words;
  private double rating;
  private int id;

  public Passage(CharSequence sentence, List<Term> terms) {
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

}
