package com.expleague.sensearch.snippet.passage;

import com.expleague.sensearch.core.Term;
import com.expleague.sensearch.index.Index;
import com.expleague.sensearch.snippet.Segment;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Maxim on 10.10.2018. Email: alvinmax@mail.ru
 */
public class Passage {

  private CharSequence sentence;
  private List<Term> words;
  private List<Segment> selection = new ArrayList<>();
  private double rating;
  private long id;

  public Passage(CharSequence sentence, Stream<Term> terms) {
    this.sentence = sentence;
    this.words = terms.collect(Collectors.toList());
    this.rating = 0;
  }

  public double getRating() {
    return rating;
  }

  public void setRating(double rating) {
    this.rating = rating;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public CharSequence getSentence() {
    return sentence;
  }

  public List<Segment> getSelection() {
    return selection;
  }

  public Stream<Term> words() {
    return words.stream();
  }
}
