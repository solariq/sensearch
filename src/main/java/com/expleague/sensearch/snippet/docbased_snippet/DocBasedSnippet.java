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
  private List<Segment> selection;

  public DocBasedSnippet(CharSequence title, List<Passage> passages, Query query) {
    this.title = title;

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < passages.size(); ++i) {
      if (i > 0 && passages.get(i-1).getId() + 1 < passages.get(i).getId()) {
        sb.append(" ... ");
      }
      sb.append(passages.get(i).getSentence());
    }
    this.content = sb;

    this.selection = query
        .getTerms()
        .stream()
        .flatMap(x -> Passages.containsSelection(content, x.getRaw())
            .stream())
        .sorted(Comparator.comparingInt(Segment::getLeft))
        .collect(Collectors.toList());
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
