package com.expleague.sensearch.snippet.experiments;

import java.util.List;

public class Result {
  private List<Annotation> annotations;
  private String document_html;
  private String document_title;
  private List<Token> document_tokens;
  private String document_url;
  private String example_id;
  private List<LongAnswerCandidate> long_answer_candidates;
  private String question_text;
  private List<String> question_tokens;

  public String getQuestion_text() {
    return question_text;
  }

  public void setQuestion_text(String question_text) {
    this.question_text = question_text;
  }

  public List<String> getQuestion_tokens() {
    return question_tokens;
  }

  public void setQuestion_tokens(List<String> question_tokens) {
    this.question_tokens = question_tokens;
  }

  public List<LongAnswerCandidate> getLong_answer_candidates() {
    return long_answer_candidates;
  }

  public void setLong_answer_candidates(
      List<LongAnswerCandidate> long_answer_candidates) {
    this.long_answer_candidates = long_answer_candidates;
  }

  public String getExample_id() {
    return example_id;
  }

  public void setExample_id(String example_id) {
    this.example_id = example_id;
  }

  public String getDocument_url() {
    return document_url;
  }

  public void setDocument_url(String document_url) {
    this.document_url = document_url;
  }

  public List<Token> getDocument_tokens() {
    return document_tokens;
  }

  public void setDocument_tokens(
      List<Token> document_tokens) {
    this.document_tokens = document_tokens;
  }

  public List<Annotation> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(
      List<Annotation> annotations) {
    this.annotations = annotations;
  }

  public String getDocument_html() {
    return document_html;
  }

  public void setDocument_html(String document_html) {
    this.document_html = document_html;
  }

  public String getDocument_title() {
    return document_title;
  }

  public void setDocument_title(String document_title) {
    this.document_title = document_title;
  }
}
