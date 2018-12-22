package com.expleague.sensearch.miner.impl;

import com.expleague.ml.meta.DSItem;
import com.expleague.sensearch.Page;
import com.expleague.sensearch.query.Query;

import java.net.URI;


public class QURLItem extends DSItem.Stub {
  private String query;
  private URI pageURI;

  private transient Page pageCache;
  private transient Query queryCache;

  public QURLItem(Page page, Query query) {
    this.query = query.text();
    this.pageURI = page.uri();
    pageCache = page;
    queryCache = query;
  }

  public Page pageCache() {
    return pageCache;
  }

  public Query queryCache() {
    return queryCache;
  }

  @Override
  public String id() {
    return query + "::" + pageURI.toString();
  }
}
