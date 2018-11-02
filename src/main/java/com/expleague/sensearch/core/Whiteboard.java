package com.expleague.sensearch.core;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.miner.Features;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.Snippet;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

public interface Whiteboard {
  @Nullable
  Stream<Pair<Page, Features>> textFeatures();
  void putTextFeatures(Stream<Pair<Page, Features>> textFeatures);

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
