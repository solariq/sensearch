package com.expleague.sensearch.core;

import com.carrotsearch.hppc.IntObjectMap;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.Snippet;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public interface Whiteboard {

  IntObjectMap<Map<Page, Features>> textFeatures();

  IntObjectMap<Map<Page, Features>> filterFeatures();

  void putTextFeatures(Map<Page, Features> textFeatures, int index);

  void putFilterFeatures(Map<Page, Features> filterFeatures, int index);

  // TODO: looks like it should be a List<...>
  @Nullable
  Map<Page, Double> pageScores();

  Map<Page, Double> pageFilterScores();

  void putPageScores(Map<Page, Double> scores);

  void putPageFilterScores(Map<Page, Double> scores);

  @Nullable
  Page[] results();

  void putResults(Page[] pages);

  @Nullable
  IntObjectMap<Page[]> subResults();

  IntObjectMap<Page[]> subFilterResults();

  void putSubResult(Page[] subResult, int index);

  void putSubFilterResult(Page[] subResult, int index);

  Snippet[] snippets();

  void putSnippets(Snippet[] snippets);

  @Nullable
  IntObjectMap<Query> query();

  void putQuery(IntObjectMap<Query> baseQuery);

  String input();

  int pageNo();

  int totalResults();

  int queriesNumber();

  void putGoogleResults(ResultItem[] googleResults);

  @Nullable
  ResultItem[] googleResults();
}
