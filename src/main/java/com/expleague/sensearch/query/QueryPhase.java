package com.expleague.sensearch.query;

import com.expleague.sensearch.core.SearchPhase;
import com.expleague.sensearch.core.Whiteboard;

public class QueryPhase implements SearchPhase {

  @Override
  public boolean test(Whiteboard whiteboard) {
    return whiteboard.input() != null;
  }

  @Override
  public void accept(Whiteboard whiteboard) {
    final String input = whiteboard.input();
    whiteboard.putQuery(
        new BaseQuery(input, whiteboard.builder().getIndex(), whiteboard.builder().getLemmer()));
  }
}
