package com.expleague.sensearch.core;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.miner.Features;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.Snippet;
import com.expleague.sensearch.web.Builder;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public interface Whiteboard {

  Builder builder();

  @Nullable
  Map<Page, Features>[] textFeatures();

  void putTextFeatures(Map<Page, Features>[] textFeatures);

  @Nullable
  Page[] results();

  void putResults(Page[] pages);

  @Nullable
  Page[][] subResults();

  void putSubResults(Page[][] subResults);

  Snippet[] snippets();

  void putSnippets(Snippet[] snippets);

  @Nullable
  Query[] query();

  void putQuery(Query[] baseQuery);

  String input();

  int pageNo();

  int totalResults();

  void putGoogleResults(ResultItem[] googleResults);

  @Nullable
  ResultItem[] googleResults();
}
