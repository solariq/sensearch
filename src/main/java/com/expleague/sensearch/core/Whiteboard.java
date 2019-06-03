package com.expleague.sensearch.core;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.SenSeArch.ResultItem;
import com.expleague.sensearch.features.Features;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.Snippet;
import gnu.trove.map.TIntObjectMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public interface Whiteboard {

  TIntObjectMap<Map<Page, Features>> textFeatures();

  TIntObjectMap<Map<Page, Features>> filterFeatures();

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
  TIntObjectMap<Page[]> subResults();

  TIntObjectMap<Page[]> subFilterResults();

  void putSubResult(Page[] subResult, int index);

  void putSubFilterResult(Page[] subResult, int index);

  Snippet[] snippets();

  void putSnippets(Snippet[] snippets);

  @Nullable
  TIntObjectMap<Query> query();

  void putQuery(TIntObjectMap<Query> baseQuery);

  String input();

  int pageNo();

  int totalResults();

  int queriesNumber();

  void putGroundTruthResults(ResultItem[] groundTruthResults);

  @Nullable
  ResultItem[] groundTruthResults();
}
