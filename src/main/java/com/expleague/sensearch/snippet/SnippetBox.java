package com.expleague.sensearch.snippet;

import com.expleague.sensearch.snippet.Snippet;

public interface SnippetBox {

  public int size();

  public boolean makeQuery(CharSequence s);

  public Snippet getSnippet(int idx);
}
