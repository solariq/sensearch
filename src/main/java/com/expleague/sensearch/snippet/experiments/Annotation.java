package com.expleague.sensearch.snippet.experiments;

import java.util.List;

public class Annotation {
  private String annotation_id;
  private LongAnswer long_answer;
  private List<ShortAnswer> short_answers;
  private String yes_no_answer;

  public String getAnnotation_id() {
    return annotation_id;
  }

  public void setAnnotation_id(String annotation_id) {
    this.annotation_id = annotation_id;
  }

  public LongAnswer getLong_answer() {
    return long_answer;
  }

  public void setLong_answer(LongAnswer long_answer) {
    this.long_answer = long_answer;
  }

  public List<ShortAnswer> getShort_answers() {
    return short_answers;
  }

  public void setShort_answers(
      List<ShortAnswer> short_answers) {
    this.short_answers = short_answers;
  }

  public String getYes_no_answer() {
    return yes_no_answer;
  }

  public void setYes_no_answer(String yen_no_answer) {
    this.yes_no_answer = yen_no_answer;
  }
}
