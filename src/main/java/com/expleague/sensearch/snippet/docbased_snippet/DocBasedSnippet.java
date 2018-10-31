package com.expleague.sensearch.snippet.docbased_snippet;

import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.Segment;
import com.expleague.sensearch.snippet.Snippet;
import com.expleague.sensearch.snippet.passage.Passage;
import com.expleague.sensearch.snippet.passage.Passages;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DocBasedSnippet implements Snippet {
  private CharSequence title;
  private CharSequence content;
  private List<Segment> selection = new ArrayList<>();

  public DocBasedSnippet(CharSequence title, List<Passage> passages, Query query) {
    this.title = title;
    this.content = passages.stream()
        .map(Passage::getSentence)
        .collect(Collectors.joining());
    this.selection = query
        .getTerms()
        .stream()
        .flatMap(x -> Passages.containsSelection(content, x.getRaw())
            .stream())
        .collect(Collectors.toList());
    selection.sort(Comparator.comparingInt(Segment::getLeft));
  }

  @Override
  public CharSequence getTitle() {
    return title;
  }

  @Override
  public CharSequence getContent() {
    return content;
  }

  @Override
  public List<Segment> getSelection() {
    return selection;
  }
}