package com.expleague.sensearch.core.impl;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.core.Whiteboard;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.Snippet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public class WhiteboardImpl implements Whiteboard {

  private final String input;
  private final int page;
  private final int queriesNumber = 1;
  private Page[] results;
  private List<Page[]> subResults = new ArrayList<>(queriesNumber);
  private List<Page[]> subFilterResults = new ArrayList<>(queriesNumber);
  private Snippet[] snippets;
  private List<Query> queries;
  private List<Map<Page, Features>> textFeatures = new ArrayList<>(queriesNumber);
  private List<Map<Page, Features>> filterFeatures = new ArrayList<>(queriesNumber);
  private ResultItem[] googleResults;
  private Map<Page, Double> pageScores;
  private Map<Page, Double> pageFilterScores;

  public WhiteboardImpl(String input, int page) {
    this.input = input;
    this.page = page;

    for (int i = 0; i < queriesNumber; ++i) {
      subResults.add(null);
      textFeatures.add(null);
      filterFeatures.add(null);
      subFilterResults.add(null);
    }
  }

  @Override
  public synchronized List<Map<Page, Features>> textFeatures() {
    return textFeatures;
  }

  @Override
  public synchronized List<Map<Page, Features>> filterFeatures() {
    return filterFeatures;
  }

  @Override
  public synchronized void putTextFeatures(Map<Page, Features> textFeatures, int index) {
      this.textFeatures.set(index, textFeatures);
  }

  @Override
  public void putFilterFeatures(Map<Page, Features> filterFeatures, int index) {
    this.filterFeatures.set(index, filterFeatures);
  }

  @Nullable
  @Override
  public synchronized Map<Page, Double> pageScores() {
    return pageScores;
  }

  @Override
  public Map<Page, Double> pageFilterScores() {
    return pageFilterScores;
  }

  @Override
  public synchronized void putPageScores(Map<Page, Double> scores) {
    this.pageScores = scores;
  }

  @Override
  public void putPageFilterScores(Map<Page, Double> scores) {
    this.pageFilterScores = scores;
  }

  @Nullable
  @Override
  public synchronized Page[] results() {
    return results;
  }

  @Override
  public synchronized void putResults(Page[] pages) {
    this.results = pages;
  }

  @Nullable
  @Override
  public synchronized List<Page[]> subResults() {
    return subResults;
  }

  @Override
  public List<Page[]> subFilterResults() {
    return subFilterResults;
  }

  @Override
  public synchronized void putSubResult(Page[] subResult, int index) {
    this.subResults.set(index, subResult);
  }

  @Override
  public void putSubFilterResult(Page[] subResult, int index) {
    subFilterResults.set(index, subResult);
  }

  @Override
  public Snippet[] snippets() {
    return this.snippets;
  }

  @Override
  public synchronized void putSnippets(Snippet[] snippets) {
    this.snippets = snippets;
  }

  @Nullable
  @Override
  public synchronized List<Query> query() {
    return queries;
  }

  @Nullable
  @Override
  public synchronized String input() {
    return input;
  }

  @Override
  public synchronized void putQuery(List<Query> query) {
    this.queries = query;
  }

  @Override
  public synchronized int pageNo() {
    return page;
  }

  @Override
  public int totalResults() {
    return results.length;
  }

  @Override
  public void putGoogleResults(ResultItem[] googleResults) {
    this.googleResults = googleResults;
  }

  @Nullable
  @Override
  public ResultItem[] googleResults() {
    return googleResults;
  }

  @Override
  public int queriesNumber() {
      return queriesNumber;
  }
}
