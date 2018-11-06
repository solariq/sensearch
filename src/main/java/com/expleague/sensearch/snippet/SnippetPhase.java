package com.expleague.sensearch.snippet;

import com.expleague.sensearch.Page;
import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SnippetPhase implements SearchPhase {
  final private SnippetsCreator snippetsCreator = new SnippetsCreator();

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.results() != null && whiteboard.query() != null;
  }


  @Override
  public void accept(Whiteboard whiteboard) {
    final List<Snippet> snippets = new ArrayList<>();
    for (Page doc : Objects.requireNonNull(whiteboard.results())) {
      snippets.add(snippetsCreator.getSnippet(doc, whiteboard.query(), whiteboard.builder().getLemmer()));
    }
    //noinspection ToArrayCallWithZeroLengthArrayArgument
    whiteboard.putSnippets(snippets.toArray(new Snippet[snippets.size()]));
  }
}
