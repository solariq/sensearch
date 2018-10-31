package com.expleague.sensearch.snippet.clustered_snippet;

import com.expleague.sensearch.snippet.passage.Passage;
import java.util.List;

/**
 * Created by Maxim on 10.10.2018. Email: alvinmax@mail.ru
 */
public class Cluster {

  List<Passage> passages;
  double rating;

  public Cluster(List<Passage> passages) {
    this.passages = passages;
    rating = passages.stream().mapToDouble(Passage::getRating).sum();
  }

  public double getRating() {
    return rating;
  }

  public List<Passage> getPassages() {
    return passages;
  }
}
