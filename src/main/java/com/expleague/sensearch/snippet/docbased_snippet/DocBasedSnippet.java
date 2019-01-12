package com.expleague.sensearch.snippet.docbased_snippet;

import com.expleague.commons.seq.CharSeqTools;
import com.expleague.sensearch.query.Query;
import com.expleague.sensearch.snippet.Segment;
import com.expleague.sensearch.snippet.Snippet;
import com.expleague.sensearch.snippet.passage.Passage;
import com.expleague.sensearch.snippet.passage.Passages;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DocBasedSnippet implements Snippet {

  private static final long MAX_LENGTH = 300; // 🚜🚜🚜

  private CharSequence title;
  private CharSequence content;
  private List<Segment> selection;

  public DocBasedSnippet(CharSequence title, List<Passage> passages, Query query) {
    this.title = title;

    double bestPassage = passages.stream().mapToDouble(Passage::getRating).max().orElse(1);

    List<Passage> bestPassages =
        passages
            .stream()
            .filter(passage -> passage.getRating() * 2 >= bestPassage)
            .sorted(Comparator.comparingLong(Passage::getId))
            .collect(Collectors.toList());

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bestPassages.size(); ++i) {
      if (sb.length() + bestPassages.get(i).getSentence().length() > MAX_LENGTH) {
        long prefix = MAX_LENGTH - sb.length();
        sb.append(bestPassages.get(i).getSentence().subSequence(0, (int) prefix)).append("...");
        break;
      }
      if (i > 0 && bestPassages.get(i - 1).getId() + 1 < bestPassages.get(i).getId()) {
        sb.append(" ... ");
      }
      sb.append(bestPassages.get(i).getSentence());
    }
    this.content = sb;

    this.selection =
        query
            .terms()
            .stream()
            .flatMap(x -> Passages.containsSelection(content, x.text()).stream())
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
