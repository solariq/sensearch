package com.expleague.sensearch.core.impl;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.Whiteboard;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.Snippet;
import org.jetbrains.annotations.Nullable;

public class WhiteboardImpl implements Whiteboard {
  private Page[] results;
  private Snippet[] snippets;
  private Query query;
  private final String input;
  private final int page;

  public WhiteboardImpl(String input, int page) {
    this.input = input;
    this.page = page;
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
  public synchronized Query query() {
    return query;
  }

  @Nullable
  @Override
  public synchronized String input() {
    return input;
  }

  @Override
  public synchronized void putQuery(Query query) {
    this.query = query;
  }

  @Override
  public synchronized int pageNo() {
    return page;
  }

  @Override
  public int totalResults() {
    return results.length;
  }
}
