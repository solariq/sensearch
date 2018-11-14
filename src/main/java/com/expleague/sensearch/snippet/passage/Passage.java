package com.expleague.sensearch.snippet.passage;

import com.expleague.commons.text.lemmer.WordInfo;
import com.expleague.sensearch.core.Lemmer;
import com.expleague.sensearch.snippet.Segment;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maxim on 10.10.2018. Email: alvinmax@mail.ru
 */
public class Passage {

  private CharSequence sentence;
  private List<WordInfo> words;
  private List<Segment> selection = new ArrayList<>();
  private double rating;
  private long id;

  public Passage(CharSequence sentence, Lemmer lemmer) {
    this.sentence = sentence;
    this.words = lemmer.myStem.parse(sentence);
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

  public List<WordInfo> getWords() {
    return words;
  }
}
