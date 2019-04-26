package com.expleague.sensearch.snippet.experiments.naturalquestions;

public class LongAnswerCandidate {
  private int end_byte;
  private int end_token;
  private int start_byte;
  private int start_token;
  private boolean top_level;

  public int getEnd_byte() {
    return end_byte;
  }

  public int getStart_byte() {
    return start_byte;
  }

  public int getEnd_token() {
    return end_token;
  }

  public int getStart_token() {
    return start_token;
  }

  public boolean getTop_level() {
    return top_level;
  }

  public void setEnd_byte(int end_byte) {
    this.end_byte = end_byte;
  }

  public void setStart_byte(int start_byte) {
    this.start_byte = start_byte;
  }

  public void setEnd_token(int end_token) {
    this.end_token = end_token;
  }

  public void setStart_token(int start_token) {
    this.start_token = start_token;
  }

  public void setTop_level(boolean top_level) {
    this.top_level = top_level;
  }
}
