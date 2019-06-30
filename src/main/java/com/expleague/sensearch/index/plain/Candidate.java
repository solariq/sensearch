package com.expleague.sensearch.index.plain;

import com.expleague.sensearch.core.PageIdUtils;

public class Candidate {

  private final long id;
  private final long pageId;
    private final double dist;

    public Candidate(long id, double dist) {
        this.id = id;
        this.dist = dist;
      pageId = PageIdUtils.toPageId(id);
    }

    public double getDist() {
        return dist;
    }

    public long getId() {
        return id;
    }

    public long getPageId() {
        return pageId;
    }
}
