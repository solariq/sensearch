package com.expleague.sensearch.snippet.experiments.naturalquestions;

public class Token {
  private int end_byte;
  private boolean html_token;
  private int start_byte;
  private String token;

  public int getStart_byte() {
    return start_byte;
  }

  public void setStart_byte(int start_byte) {
    this.start_byte = start_byte;
  }

  public int getEnd_byte() {
    return end_byte;
  }

  public void setEnd_byte(int end_byte) {
    this.end_byte = end_byte;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public boolean getHtml_token() {
    return html_token;
  }

  public void setHtml_token(boolean html_token) {
    this.html_token = html_token;
  }
}
