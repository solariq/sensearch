package com.expleague.sensearch.core;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.Snippet;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Whiteboard {
  @Nullable
  Page[] results();
  void putResults(Page[] pages);

  Snippet[] snippets();
  void putSnippets(Snippet[] snippets);

  @Nullable
  Query query();
  void putQuery(Query baseQuery);

  String input();
  int pageNo();

  int totalResults();
}