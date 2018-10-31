package components.snippeter;

import java.util.List;

public interface Snippet {

  CharSequence getTitle();

  CharSequence getContent();

  List<Segment> getSelection();
}
