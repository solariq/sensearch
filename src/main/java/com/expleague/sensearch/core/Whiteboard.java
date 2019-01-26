package com.expleague.sensearch.core;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.miner.Features;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.Snippet;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public interface Whiteboard {
  @Nullable
  List<Map<Page, Features>> textFeatures();

  void putTextFeatures(List<Map<Page, Features>> textFeatures);

  void putTextFeatures(Map<Page, Features> textFeatures, int index);

  // TODO: looks like it should be a List<...>
  @Nullable
  Map<Page, Double> pageScores();

  void putPageScores(Map<Page, Double> scores);

  @Nullable
  Page[] results();

  void putResults(Page[] pages);

  @Nullable
  List<Page[]> subResults();

  void putSubResults(List<Page[]> subResults);

  void putSubResult(Page[] subResult, int index);

  Snippet[] snippets();

  void putSnippets(Snippet[] snippets);

  @Nullable
  List<Query> query();

  void putQuery(List<Query> baseQuery);

  String input();

  int pageNo();

  int totalResults();

  int queriesNumber();

  void putGoogleResults(ResultItem[] googleResults);

  @Nullable
  ResultItem[] googleResults();
}
