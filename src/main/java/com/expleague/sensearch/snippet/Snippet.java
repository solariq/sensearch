package com.expleague.sensearch.snippet;

import java.util.List;

public interface Snippet {

  CharSequence getTitle();

  CharSequence getContent();

  List<Segment> getSelection();
}
