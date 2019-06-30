package com.expleague.sensearch.donkey.embedding;

import com.expleague.commons.math.vectors.Vec;
import java.util.List;

public class EmbeddedPage {

  private final long pageId;
  private final List<Vec> titleVecs;
  private final List<Vec> textVecs;
  private final List<Vec> linkVecs;

  EmbeddedPage(long pageId, List<Vec> titleVecs, List<Vec> textVecs, List<Vec> linkVecs) {
    this.pageId = pageId;
    this.titleVecs = titleVecs;
    this.textVecs = textVecs;
    this.linkVecs = linkVecs;
  }

  public long pageId() {
    return pageId;
  }

  public List<Vec> titleVecs() {
    return titleVecs;
  }

  public List<Vec> textVecs() {
    return textVecs;
  }

  public List<Vec> linkVecs() {
    return linkVecs;
  }

}
