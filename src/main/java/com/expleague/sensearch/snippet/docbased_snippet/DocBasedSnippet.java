package com.expleague.sensearch.snippet.docbased_snippet;

import com.expleague.sensearch.core.Term;
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

  private static final int MAX_LENGTH = 300; // 🚜🚜🚜

  private final CharSequence title;
  private final CharSequence content;
  private final List<Segment> selection = new ArrayList<>();

  public DocBasedSnippet(CharSequence title, List<Passage> passages, Query query) {
    this.title = title;

    double bestPassage = passages.stream().mapToDouble(Passage::rating).max().orElse(1);

    List<Passage> bestPassages =
        passages
            .stream()
            .filter(passage -> passage.rating() * 2 >= bestPassage)
            .sorted(Comparator.comparingLong(Passage::id))
            .collect(Collectors.toList());

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bestPassages.size(); ++i) {
      CharSequence sentence = bestPassages.get(i).sentence();
      if (sb.length() + sentence.length() > MAX_LENGTH) {
        sb.append(sentence.subSequence(0, Math.min(MAX_LENGTH, sentence.length()))).append("...");
        break;
      }
      if (i > 0 && bestPassages.get(i - 1).id() + 1 < bestPassages.get(i).id()) {
        sb.append(" ... ");
      }
      sb.append(sentence);
    }
    content = sb;

    selection.addAll(
        query
            .terms()
            .stream()
            .flatMap(x -> Passages.containsSelection(content, x.text()).stream())
            .collect(Collectors.toList()));

    selection.addAll(
        query
            .terms()
            .stream()
            .map(Term::lemma)
            .flatMap(x -> Passages.containsSelection(content, x.text()).stream())
            .collect(Collectors.toList()));

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
