package components.snippeter.passage;

import components.query.Query;
import components.snippeter.Segment;
import components.snippeter.passage.Passages;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maxim on 10.10.2018. Email: alvinmax@mail.ru
 */
public class Passage {

  private CharSequence sentence;
  private List<Segment> selection = new ArrayList<>();
  private double rating;
  private long id;

  public Passage(CharSequence sentence) {
    this.sentence = sentence;
    this.rating = 0;
  }

  public double getRating() {
    return rating;
  }

  public long getId() {
    return id;
  }

  public void setRating(double rating) {
    this.rating = rating;
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
}
