package com.expleague.sensearch.miner.impl;

import com.expleague.ml.meta.DSItem;
import java.net.URI;


public class QURLItem extends DSItem.Stub {

  private String query;
  private URI pageURI;

  public QURLItem(String query, URI page) {
    this.query = query;
    this.pageURI = page;
  }


  @Override
  public String id() {
    return query + "::" + pageURI.toString();
  }
}
